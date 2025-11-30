// Follow this setup guide to integrate the Deno runtime into your app:
// https://deno.land/manual/getting_started/setup_your_environment

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

interface RequestBody {
  phoneNumber: string
  otpCode: string
}

serve(async (req) => {
  try {
    const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!
    const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

    // Parse request body
    const { phoneNumber, otpCode }: RequestBody = await req.json()

    if (!phoneNumber || !otpCode) {
      return new Response(
        JSON.stringify({ error: 'Phone number and OTP code are required' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Create Supabase client
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

    // Find the OTP code
    const { data: otpData, error: fetchError } = await supabase
      .from('otp_codes')
      .select('*')
      .eq('phone_number', phoneNumber)
      .eq('code', otpCode)
      .is('verified_at', null)
      .gt('expires_at', new Date().toISOString())
      .order('created_at', { ascending: false })
      .limit(1)
      .single()

    if (fetchError || !otpData) {
      return new Response(
        JSON.stringify({ 
          error: 'Invalid or expired OTP code',
          code: 'INVALID_OTP'
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Check if max attempts exceeded
    if (otpData.attempts >= 5) {
      return new Response(
        JSON.stringify({ 
          error: 'Maximum verification attempts exceeded',
          code: 'MAX_ATTEMPTS'
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }

    // Mark OTP as verified
    const { error: updateError } = await supabase
      .from('otp_codes')
      .update({ 
        verified_at: new Date().toISOString(),
        attempts: otpData.attempts + 1
      })
      .eq('id', otpData.id)

    if (updateError) {
      console.error('Failed to update OTP:', updateError)
      throw new Error('Failed to verify OTP')
    }

    // Check if user exists in user_profiles
    const { data: existingProfile } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('phone_number', phoneNumber)
      .single()

    // If user doesn't exist, create a new profile
    let userId = existingProfile?.id

    if (!existingProfile) {
      // Check if user exists in auth.users
      const { data: existingAuthUsers } = await supabase.auth.admin.listUsers()
      const existingAuthUser = existingAuthUsers.users.find(u => u.phone === phoneNumber)
      
      if (existingAuthUser) {
        // User exists in auth but not in profiles, use existing ID
        userId = existingAuthUser.id
      } else {
        // Create user in auth.users first (using service role)
        const { data: authData, error: authError } = await supabase.auth.admin.createUser({
          phone: phoneNumber,
          phone_confirm: true,
          user_metadata: {
            phone_verified: true
          }
        })

        if (authError) {
          console.error('Failed to create auth user:', authError)
          throw new Error('Failed to create user')
        }

        userId = authData.user.id
      }

      // Create user profile
      const { error: profileError } = await supabase
        .from('user_profiles')
        .insert({
          id: userId,
          phone_number: phoneNumber,
          is_verified: true
        })

      if (profileError) {
        console.error('Failed to create user profile:', profileError)
      }
    }

    // Generate session for the user (existing or new)
    const { data: sessionData, error: sessionError } = await supabase.auth.admin.generateLink({
      type: 'magiclink',
      email: `${phoneNumber.replace('+', '')}@temp.momoterminal.com`,
      options: {
        redirectTo: 'momoterminal://auth/callback'
      }
    })

    if (sessionError) {
      console.error('Session generation error:', sessionError)
    }

    return new Response(
      JSON.stringify({
        success: true,
        message: 'OTP verified successfully',
        userId: userId,
        isNewUser: !existingProfile,
        sessionToken: sessionData?.properties?.hashed_token || null
      }),
      { 
        status: 200, 
        headers: { 'Content-Type': 'application/json' } 
      }
    )

  } catch (error) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ 
        error: error instanceof Error ? error.message : 'Internal server error' 
      }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
