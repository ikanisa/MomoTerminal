import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verify } from "https://deno.land/x/djwt@v2.8/mod.ts"

const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface CompleteProfileRequest {
  // userId is now extracted from JWT
  pin: string
  merchantName: string
  acceptedTerms: boolean
}

interface CompleteProfileResponse {
  success: boolean
  message?: string
  error?: string
  code?: string
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, { headers: CORS_HEADERS })
  }

  try {
    // Initialize Supabase client
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const JWT_SECRET = Deno.env.get('SUPABASE_JWT_SECRET') || Deno.env.get('JWT_SECRET')!
    
    // Verify JWT
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      throw new Error('Missing Authorization header')
    }
    const token = authHeader.replace('Bearer ', '')
    const keyData = new TextEncoder().encode(JWT_SECRET)
    const cryptoKey = await crypto.subtle.importKey(
      "raw", 
      keyData, 
      { name: "HMAC", hash: "SHA-256" }, 
      true, 
      ["verify"]
    )
    
    const payload = await verify(token, cryptoKey)
    const userId = payload.sub
    
    if (!userId) {
       throw new Error('Invalid token: missing subject')
    }

    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Parse request body
    const { pin, merchantName, acceptedTerms }: CompleteProfileRequest = await req.json()

    console.log(`[PROFILE-COMPLETE] Completing profile for user: ${userId}`)

    // Validate inputs
    if (!pin || !merchantName) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Missing required fields',
          code: 'INVALID_INPUT'
        }),
        { status: 400, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    // Validate PIN format (6 digits)
    if (!/^\d{6}$/.test(pin)) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'PIN must be 6 digits',
          code: 'INVALID_PIN'
        }),
        { status: 400, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    // Validate merchant name
    if (merchantName.trim().length < 2) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Merchant name must be at least 2 characters',
          code: 'INVALID_MERCHANT_NAME'
        }),
        { status: 400, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    // Hash the PIN using SHA-256
    const encoder = new TextEncoder()
    const pinData = encoder.encode(pin)
    const hashBuffer = await crypto.subtle.digest('SHA-256', pinData)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    const pinHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')

    console.log(`[PROFILE-COMPLETE] PIN hashed, updating profile...`)

    // Update user profile
    const { data: profile, error: updateError } = await supabase
      .from('user_profiles')
      .update({
        merchant_name: merchantName.trim(),
        pin_hash: pinHash,
        terms_accepted_at: acceptedTerms ? new Date().toISOString() : null,
        updated_at: new Date().toISOString()
      })
      .eq('id', userId)
      .select()
      .single()

    if (updateError) {
      console.error('[PROFILE-COMPLETE] Update error:', updateError)
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Failed to update profile',
          code: 'UPDATE_ERROR'
        }),
        { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    console.log(`[PROFILE-COMPLETE] Profile completed successfully for user: ${userId}`)

    const response: CompleteProfileResponse = {
      success: true,
      message: 'Profile completed successfully'
    }

    return new Response(
      JSON.stringify(response),
      { status: 200, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error('[PROFILE-COMPLETE] Unexpected error:', error)
    return new Response(
      JSON.stringify({
        success: false,
        error: 'An unexpected error occurred',
        code: 'INTERNAL_ERROR'
      }),
      { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
    )
  }
})
