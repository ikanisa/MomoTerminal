// Supabase Edge Function: verify-whatsapp-otp
// Verifies WhatsApp OTP and creates/authenticates user

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

interface RequestBody {
  phoneNumber: string
  otpCode: string
}

interface VerifyOtpResponse {
  success: boolean
  message: string
  userId?: string
  isNewUser?: boolean
  accessToken?: string
  refreshToken?: string
  expiresIn?: number
  error?: string
  code?: string
}

serve(async (req) => {
  try {
    const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!
    const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

    // Parse and validate request
    const { phoneNumber, otpCode }: RequestBody = await req.json()

    if (!phoneNumber || !otpCode) {
      return new Response(
        JSON.stringify({ 
          error: 'Phone number and OTP code are required',
          code: 'MISSING_PARAMS'
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Create Supabase client with service role
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

    console.log(`Verifying OTP for phone: ${phoneNumber}`)

    // Step 1: Find and validate OTP code
    const { data: otpData, error: fetchError } = await supabase
      .from('otp_codes')
      .select('*')
      .eq('phone_number', phoneNumber)
      .eq('code', otpCode)
      .is('verified_at', null)
      .gt('expires_at', new Date().toISOString())
      .order('created_at', { ascending: false })
      .limit(1)
      .maybeSingle()

    if (fetchError) {
      console.error('OTP fetch error:', fetchError)
      return new Response(
        JSON.stringify({ 
          error: 'Database error',
          code: 'DB_ERROR'
        }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    if (!otpData) {
      console.log(`Invalid OTP: ${otpCode} for ${phoneNumber}`)
      return new Response(
        JSON.stringify({ 
          error: 'Invalid or expired OTP code',
          code: 'INVALID_OTP'
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Check max attempts
    if (otpData.attempts >= 5) {
      console.log(`Max attempts exceeded for ${phoneNumber}`)
      return new Response(
        JSON.stringify({ 
          error: 'Maximum verification attempts exceeded',
          code: 'MAX_ATTEMPTS'
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Step 2: Mark OTP as verified
    const { error: updateError } = await supabase
      .from('otp_codes')
      .update({ 
        verified_at: new Date().toISOString(),
        attempts: otpData.attempts + 1
      })
      .eq('id', otpData.id)

    if (updateError) {
      console.error('Failed to update OTP:', updateError)
      return new Response(
        JSON.stringify({ 
          error: 'Failed to verify OTP',
          code: 'UPDATE_ERROR'
        }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }

    console.log(`OTP verified for ${phoneNumber}`)

    // Step 3: Check if user exists in user_profiles
    const { data: existingProfile, error: profileError } = await supabase
      .from('user_profiles')
      .select('id')
      .eq('phone_number', phoneNumber)
      .maybeSingle()

    if (profileError && profileError.code !== 'PGRST116') {
      console.error('Profile lookup error:', profileError)
    }

    let userId: string
    let isNewUser = false

    if (existingProfile) {
      // Existing user - get their ID
      userId = existingProfile.id
      console.log(`Existing user found: ${userId}`)
    } else {
      // New user - create auth user and profile
      isNewUser = true
      console.log(`Creating new user for ${phoneNumber}`)

      // Create user in auth.users
      const { data: authData, error: authError } = await supabase.auth.admin.createUser({
        phone: phoneNumber,
        phone_confirm: true,
        user_metadata: {
          phone_verified: true,
          verified_at: new Date().toISOString()
        }
      })

      if (authError) {
        console.error('Auth user creation error:', authError)
        
        // If user already exists in auth, try to get them
        if (authError.message?.includes('already registered')) {
          console.log('User exists in auth, fetching...')
          const { data: users, error: listError } = await supabase.auth.admin.listUsers({
            page: 1,
            perPage: 1000
          })
          
          if (!listError && users) {
            const existingUser = users.users.find(u => u.phone === phoneNumber)
            if (existingUser) {
              userId = existingUser.id
              console.log(`Found existing auth user: ${userId}`)
            } else {
              throw new Error('User exists but could not be found')
            }
          } else {
            throw new Error(`Failed to create user: ${authError.message}`)
          }
        } else {
          throw new Error(`Failed to create user: ${authError.message}`)
        }
      } else {
        userId = authData.user.id
        console.log(`Created new auth user: ${userId}`)
      }

      // Create user profile (with upsert to handle race conditions)
      const { error: insertError } = await supabase
        .from('user_profiles')
        .upsert({
          id: userId!,
          phone_number: phoneNumber,
          is_verified: true
        }, {
          onConflict: 'id',
          ignoreDuplicates: false
        })

      if (insertError) {
        console.error('Profile creation error:', insertError)
        // Don't fail here - profile can be created later
      } else {
        console.log(`Created user profile for ${userId}`)
      }
    }

    // Step 4: Create session for the user
    console.log(`Creating session for user ${userId}`)
    
    const { data: sessionData, error: sessionError } = await supabase.auth.admin.createSession({
      user_id: userId!,
      expiresIn: 3600 * 24 * 7 // 7 days
    })

    if (sessionError) {
      console.error('Session creation error:', sessionError)
      // Return success anyway - user is verified
      return new Response(
        JSON.stringify({
          success: true,
          message: 'OTP verified successfully',
          userId: userId,
          isNewUser: isNewUser,
          code: 'SESSION_ERROR'
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    }

    console.log(`Session created successfully for ${userId}`)

    // Return success with session tokens
    const response: VerifyOtpResponse = {
      success: true,
      message: 'OTP verified successfully',
      userId: userId!,
      isNewUser: isNewUser,
      accessToken: sessionData.session.access_token,
      refreshToken: sessionData.session.refresh_token,
      expiresIn: sessionData.session.expires_in
    }

    return new Response(
      JSON.stringify(response),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('Unexpected error:', error)
    return new Response(
      JSON.stringify({ 
        error: error instanceof Error ? error.message : 'Internal server error',
        code: 'INTERNAL_ERROR'
      }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
