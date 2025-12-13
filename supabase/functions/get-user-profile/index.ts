// Edge Function: get-user-profile
// Fetches user profile from user_profiles table
// SINGLE SOURCE OF TRUTH for user data

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { verify } from "https://deno.land/x/djwt@v2.8/mod.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface GetProfileRequest {
  // userId is now extracted from JWT
}

interface UserProfile {
  id: string
  phoneNumber: string
  merchantName: string | null
  countryCode: string | null
  momoCountryCode: string | null
  momoPhone: string | null
  useMomoCode: boolean
  biometricEnabled: boolean
  nfcTerminalEnabled: boolean
  language: string
  createdAt: string | null
  updatedAt: string | null
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Verify JWT
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const JWT_SECRET = Deno.env.get('SUPABASE_JWT_SECRET') || Deno.env.get('JWT_SECRET')!

    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(
        JSON.stringify({ success: false, error: 'Missing Authorization header', code: 'UNAUTHORIZED' }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 401 }
      )
    }

    const token = authHeader.replace('Bearer ', '')
    const keyData = new TextEncoder().encode(JWT_SECRET)
    const cryptoKey = await crypto.subtle.importKey("raw", keyData, { name: "HMAC", hash: "SHA-256" }, true, ["verify"])
    
    let userId: string
    try {
      const payload = await verify(token, cryptoKey)
      userId = payload.sub as string
    } catch (e) {
      return new Response(
        JSON.stringify({ success: false, error: 'Invalid token', code: 'UNAUTHORIZED' }),
        { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 401 }
      )
    }

    // Create Supabase client with service role for data access
    const supabaseClient = createClient(supabaseUrl, supabaseServiceKey, {
        auth: {
          autoRefreshToken: false,
          persistSession: false
        }
    })

    console.log(`Fetching profile for user: ${userId}`)

    // Fetch user profile from canonical table: user_profiles
    const { data, error } = await supabaseClient
      .from('user_profiles')
      .select('*')
      .eq('id', userId)
      .single()

    if (error) {
      // User not found or other error
      console.error('Failed to fetch profile:', error)
      
      if (error.code === 'PGRST116') {
        // No rows returned - user profile doesn't exist yet
        return new Response(
          JSON.stringify({
            success: false,
            error: 'User profile not found',
            code: 'PROFILE_NOT_FOUND'
          }),
          {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 404
          }
        )
      }

      return new Response(
        JSON.stringify({
          success: false,
          error: error.message,
          code: 'FETCH_FAILED'
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500
        }
      )
    }

    // Map database columns to camelCase for client
    const profile: UserProfile = {
      id: data.id,
      phoneNumber: data.phone_number,
      merchantName: data.merchant_name,
      countryCode: data.country_code || 'RW',
      momoCountryCode: data.momo_country_code || data.country_code || 'RW',
      momoPhone: data.momo_phone || data.phone_number,
      useMomoCode: data.use_momo_code || false,
      biometricEnabled: data.biometric_enabled || false,
      nfcTerminalEnabled: data.nfc_terminal_enabled || false,
      language: data.language || 'en',
      createdAt: data.created_at,
      updatedAt: data.updated_at
    }

    console.log(`Profile fetched successfully for user: ${userId}`)

    return new Response(
      JSON.stringify({
        success: true,
        profile
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200
      }
    )

  } catch (error) {
    console.error('Get profile error:', error)
    return new Response(
      JSON.stringify({
        success: false,
        error: error.message || 'Internal server error',
        code: 'INTERNAL_ERROR'
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500
      }
    )
  }
})
