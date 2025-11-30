// Supabase Edge Function: verify-whatsapp-otp
// Verifies WhatsApp OTP and creates/authenticates user

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

// CORS configuration
const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*', // TODO: Restrict to your domain in production
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, apikey, x-client-info',
  'Access-Control-Max-Age': '86400',
}

// Rate limiting for verification attempts
const VERIFY_RATE_LIMITS = {
  PER_PHONE_5MIN: 10,    // Max 10 verification attempts per phone per 5 minutes
  PER_IP_HOUR: 100,      // Max 100 verification attempts per IP per hour
}

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

// Helper to extract client IP
function getClientIP(req: Request): string {
  return req.headers.get('x-forwarded-for')?.split(',')[0]?.trim() 
    || req.headers.get('x-real-ip')
    || 'unknown'
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { 
      status: 204,
      headers: CORS_HEADERS 
    })
  }

  try {
    const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!
    const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

    // Parse and validate request
    const { phoneNumber, otpCode }: RequestBody = await req.json()

    // Get client IP for rate limiting and security logging
    const clientIP = getClientIP(req)
    console.log(`OTP verification from IP: ${clientIP} for phone: ${phoneNumber}`)

    // Validate phone number format (E.164)
    const phoneRegex = /^\+[1-9]\d{9,14}$/
    if (!phoneNumber || !phoneRegex.test(phoneNumber)) {
      return new Response(
        JSON.stringify({ 
          error: 'Invalid phone number format',
          code: 'INVALID_PHONE_FORMAT'
        }),
        { status: 400, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    // Validate OTP format (exactly 6 digits)
    const otpRegex = /^\d{6}$/
    if (!otpCode || !otpRegex.test(otpCode)) {
      return new Response(
        JSON.stringify({ 
          error: 'OTP must be exactly 6 digits',
          code: 'INVALID_OTP_FORMAT'
        }),
        { status: 400, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    // Create Supabase client with service role
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

    // === VERIFICATION RATE LIMITING ===
    
    // 1. Per-phone verification attempts (10 per 5 minutes)
    const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString()
    const { count: recentVerifyCount } = await supabase
      .from('otp_request_logs')
      .select('*', { count: 'exact', head: true })
      .eq('phone_number', phoneNumber)
      .eq('request_type', 'verify_otp')
      .gte('created_at', fiveMinutesAgo)

    if (recentVerifyCount && recentVerifyCount >= VERIFY_RATE_LIMITS.PER_PHONE_5MIN) {
      console.log(`Verification rate limit exceeded for phone ${phoneNumber}`)
      
      return new Response(
        JSON.stringify({ 
          error: 'Too many verification attempts. Please wait before trying again.',
          code: 'RATE_LIMIT_VERIFY_PHONE',
          retryAfter: 300
        }),
        { 
          status: 429, 
          headers: { 
            ...CORS_HEADERS,
            'Content-Type': 'application/json',
            'Retry-After': '300'
          } 
        }
      )
    }

    // 2. Per-IP verification attempts (100 per hour)
    if (clientIP !== 'unknown') {
      const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000).toISOString()
      const { count: recentIPVerifyCount } = await supabase
        .from('otp_request_logs')
        .select('*', { count: 'exact', head: true })
        .eq('ip_address', clientIP)
        .eq('request_type', 'verify_otp')
        .gte('created_at', oneHourAgo)

      if (recentIPVerifyCount && recentIPVerifyCount >= VERIFY_RATE_LIMITS.PER_IP_HOUR) {
        console.log(`Verification rate limit exceeded for IP ${clientIP}`)
        
        return new Response(
          JSON.stringify({ 
            error: 'Too many verification attempts from this location.',
            code: 'RATE_LIMIT_VERIFY_IP',
            retryAfter: 3600
          }),
          { 
            status: 429, 
            headers: { 
              ...CORS_HEADERS,
              'Content-Type': 'application/json',
              'Retry-After': '3600'
            } 
          }
        )
      }
    }

    // Log verification attempt for rate limiting and analytics
    const { error: logError } = await supabase
      .from('otp_request_logs')
      .insert({
        phone_number: phoneNumber,
        ip_address: clientIP,
        user_agent: req.headers.get('user-agent') || 'unknown',
        request_type: 'verify_otp'
      })
    
    if (logError) {
      console.error('Failed to log verification attempt:', logError)
    }

    console.log(`Verifying OTP for phone: ${phoneNumber}`)

    // Hash the provided OTP for comparison
    const encoder = new TextEncoder()
    const data = encoder.encode(otpCode + phoneNumber)
    const hashBuffer = await crypto.subtle.digest('SHA-256', data)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    const otpHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')

    // Step 1: Find and validate OTP code using hash comparison
    console.log(`Looking for OTP hash for phone: ${phoneNumber}`)
    const { data: otpData, error: fetchError } = await supabase
      .from('otp_codes')
      .select('*')
      .eq('phone_number', phoneNumber)
      .eq('code', otpHash) // Compare hashes instead of plaintext
      .is('verified_at', null)
      .gt('expires_at', new Date().toISOString())
      .lt('attempts', 5) // Atomic check - must be less than 5
      .order('created_at', { ascending: false })
      .limit(1)
      .maybeSingle()

    if (fetchError) {
      console.error('OTP fetch error:', fetchError)
      console.error('OTP fetch error details:', JSON.stringify(fetchError))
      return new Response(
        JSON.stringify({ 
          error: 'Unable to verify OTP. Please try again.',
          code: 'DB_ERROR'
        }),
        { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    if (!otpData) {
      console.log(`Invalid OTP for ${phoneNumber}`)
      console.log(`Current time: ${new Date().toISOString()}`)
      
      // Increment attempts for all recent OTPs (rate limiting)
      const { error: incrementError } = await supabase.rpc('increment_otp_attempts', { 
        p_phone_number: phoneNumber 
      })
      
      if (incrementError) {
        console.error('Failed to increment attempts:', incrementError)
      }
      
      return new Response(
        JSON.stringify({ 
          error: 'Invalid or expired OTP code',
          code: 'INVALID_OTP'
        }),
        { status: 400, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    // Step 2: Atomically mark OTP as verified and increment attempts
    // This prevents race conditions
    const { data: updatedOtp, error: updateError } = await supabase
      .from('otp_codes')
      .update({ 
        verified_at: new Date().toISOString(),
        attempts: otpData.attempts + 1
      })
      .eq('id', otpData.id)
      .is('verified_at', null) // Ensure it hasn't been verified by another request
      .select()
      .maybeSingle()

    if (updateError || !updatedOtp) {
      console.error('Failed to update OTP:', updateError)
      return new Response(
        JSON.stringify({ 
          error: 'Unable to verify OTP. Please try again.',
          code: 'UPDATE_ERROR'
        }),
        { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
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
        
        // If user already exists in auth, use getUserByPhone for efficient lookup
        if (authError.message?.includes('already registered')) {
          console.log('User exists in auth, fetching by phone...')
          
          // Use direct lookup instead of listing all users
          const { data: existingAuthUser, error: getUserError } = await supabase.rpc(
            'get_user_id_by_phone',
            { phone: phoneNumber }
          )
          
          if (getUserError || !existingAuthUser) {
            console.error('Failed to get user by phone:', getUserError)
            throw new Error('User exists but could not be retrieved')
          }
          
          userId = existingAuthUser
          console.log(`Found existing auth user: ${userId}`)
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

    // Step 4: Generate access tokens for the user
    console.log(`Generating session for user ${userId}`)
    
    // Use the admin API to generate a link which contains the access/refresh tokens
    const { data: linkData, error: linkError } = await supabase.auth.admin.generateLink({
      type: 'magiclink',
      email: `${userId}@temp.local`, // Temporary email for phone-only auth
      options: {
        redirectTo: 'app://callback'
      }
    })

    if (linkError || !linkData) {
      console.error('Link generation error:', linkError)
      
      // Revert OTP verification since session creation failed
      const { error: revertError } = await supabase
        .from('otp_codes')
        .update({ verified_at: null })
        .eq('id', otpData.id)
      
      if (revertError) {
        console.error('Failed to revert OTP:', revertError)
      }
      
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Authentication failed. Please try again.',
          code: 'SESSION_ERROR'
        }),
        { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    console.log(`Session generated successfully for ${userId}`)

    // Extract tokens from the properties
    const accessToken = linkData.properties?.access_token
    const refreshToken = linkData.properties?.refresh_token
    const expiresIn = linkData.properties?.expires_in || 3600

    // Return success with session tokens
    const response: VerifyOtpResponse = {
      success: true,
      message: 'OTP verified successfully',
      userId: userId!,
      isNewUser: isNewUser,
      accessToken: accessToken!,
      refreshToken: refreshToken!,
      expiresIn: expiresIn
    }

    return new Response(
      JSON.stringify(response),
      { status: 200, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('Unexpected error:', error)
    console.error('Error stack:', error instanceof Error ? error.stack : 'No stack trace')
    return new Response(
      JSON.stringify({ 
        error: 'An unexpected error occurred. Please try again.',
        code: 'INTERNAL_ERROR'
      }),
      { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
    )
  }
})
