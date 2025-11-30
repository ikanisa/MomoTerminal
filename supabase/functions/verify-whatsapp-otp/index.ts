// Supabase Edge Function: verify-whatsapp-otp
// Verifies WhatsApp OTP and creates/authenticates user

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

// CORS headers for cross-origin requests
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
}

// Input validation patterns
const phoneRegex = /^\+[1-9]\d{9,14}$/
const otpRegex = /^\d{6}$/

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

// Helper function to hash OTP with SHA-256 (must match send-whatsapp-otp)
async function hashOTP(otpCode: string, phoneNumber: string): Promise<string> {
  const encoder = new TextEncoder()
  const data = encoder.encode(otpCode + phoneNumber)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: corsHeaders })
  }

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
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Validate phone number format
    if (!phoneRegex.test(phoneNumber)) {
      return new Response(
        JSON.stringify({ error: 'Invalid phone number format', code: 'INVALID_PHONE' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Validate OTP format
    if (!otpRegex.test(otpCode)) {
      return new Response(
        JSON.stringify({ error: 'OTP must be 6 digits', code: 'INVALID_OTP_FORMAT' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Create Supabase client with service role
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

    console.log(`Verifying OTP for phone: ${phoneNumber}`)

    // Hash the provided OTP to compare with stored hash
    const hashedOtp = await hashOTP(otpCode, phoneNumber)

    // Step 1: Atomically increment attempts and check if under limit
    // First find valid OTP records for this phone number
    const { data: pendingOtps, error: fetchError } = await supabase
      .from('otp_codes')
      .select('id, attempts, code')
      .eq('phone_number', phoneNumber)
      .is('verified_at', null)
      .gt('expires_at', new Date().toISOString())
      .lt('attempts', 5)
      .order('created_at', { ascending: false })
      .limit(5)

    if (fetchError) {
      console.error('OTP fetch error:', fetchError)
      return new Response(
        JSON.stringify({ 
          error: 'Verification failed. Please try again.',
          code: 'DB_ERROR'
        }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Find matching OTP from pending ones
    const matchingOtp = pendingOtps?.find(otp => otp.code === hashedOtp)

    if (!matchingOtp) {
      // Atomically increment attempt counter on the most recent OTP if exists
      // Use .lt('attempts', 5) to prevent race conditions
      if (pendingOtps && pendingOtps.length > 0) {
        await supabase
          .from('otp_codes')
          .update({ attempts: pendingOtps[0].attempts + 1 })
          .eq('id', pendingOtps[0].id)
          .lt('attempts', 5)
      }

      console.log(`Invalid OTP for ${phoneNumber}`)
      return new Response(
        JSON.stringify({ 
          error: 'Invalid or expired OTP code',
          code: 'INVALID_OTP'
        }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Atomically update: mark as verified and increment attempts, only if attempts < 5
    const { data: updatedOtp, error: updateError } = await supabase
      .from('otp_codes')
      .update({ 
        verified_at: new Date().toISOString(),
        attempts: matchingOtp.attempts + 1
      })
      .eq('id', matchingOtp.id)
      .lt('attempts', 5)
      .select()
      .single()

    if (updateError || !updatedOtp) {
      console.error('Failed to update OTP:', updateError)
      return new Response(
        JSON.stringify({ 
          error: 'Maximum verification attempts exceeded',
          code: 'MAX_ATTEMPTS'
        }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
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
        
        // If user already exists in auth, try to find them
        if (authError.message?.includes('already registered')) {
          console.log('User exists in auth, searching...')
          
          // Use listUsers with small page size as fallback
          // Note: Supabase doesn't have getUserByPhone, so we must list users
          const { data: { users }, error: listError } = await supabase.auth.admin.listUsers({
            page: 1,
            perPage: 100
          })
          
          if (!listError && users) {
            const foundUser = users.find(u => u.phone === phoneNumber)
            if (foundUser) {
              userId = foundUser.id
              console.log(`Found existing auth user: ${userId}`)
            } else {
              throw new Error('User exists but could not be found')
            }
          } else {
            throw new Error(`Failed to find user: ${authError.message}`)
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
      
      // Revert OTP verification status since session creation failed
      await supabase
        .from('otp_codes')
        .update({ verified_at: null })
        .eq('id', updatedOtp.id)
      
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Authentication failed. Please try again.',
          code: 'SESSION_ERROR'
        }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
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
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('Unexpected error:', error)
    return new Response(
      JSON.stringify({ 
        error: 'An error occurred. Please try again.',
        code: 'INTERNAL_ERROR'
      }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
