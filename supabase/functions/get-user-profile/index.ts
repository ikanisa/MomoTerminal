// Edge Function: get-user-profile
// Fetches user profile from user_profiles table
// SINGLE SOURCE OF TRUTH for user data

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface GetProfileRequest {
  userId: string
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
    // Create Supabase client with service role for data access
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
      {
        auth: {
          autoRefreshToken: false,
          persistSession: false
        }
      }
    )

    const body: GetProfileRequest = await req.json()
    
    // Validate required field
    if (!body.userId) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'User ID is required',
          code: 'MISSING_USER_ID'
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400
        }
      )
    }

    console.log(`Fetching profile for user: ${body.userId}`)

    // Fetch user profile from canonical table: user_profiles
    const { data, error } = await supabaseClient
      .from('user_profiles')
      .select('*')
      .eq('id', body.userId)
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

    console.log(`Profile fetched successfully for user: ${body.userId}`)

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
