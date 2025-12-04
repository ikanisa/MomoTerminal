// Edge Function: update-user-profile
// Updates user profile with MoMo configuration and settings

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface UpdateProfileRequest {
  userId: string
  countryCode?: string
  momoCountryCode?: string
  momoPhone?: string
  useMomoCode?: boolean
  merchantName?: string
  biometricEnabled?: boolean
  nfcTerminalEnabled?: boolean
  language?: string
}

serve(async (req) => {
  // Handle CORS
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Create Supabase client with service role
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

    const body: UpdateProfileRequest = await req.json()
    
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

    // Build update object with only provided fields
    const updateData: any = {}
    if (body.countryCode !== undefined) updateData.country_code = body.countryCode
    if (body.momoCountryCode !== undefined) updateData.momo_country_code = body.momoCountryCode
    if (body.momoPhone !== undefined) updateData.momo_phone = body.momoPhone
    if (body.useMomoCode !== undefined) updateData.use_momo_code = body.useMomoCode
    if (body.merchantName !== undefined) updateData.merchant_name = body.merchantName
    if (body.biometricEnabled !== undefined) updateData.biometric_enabled = body.biometricEnabled
    if (body.nfcTerminalEnabled !== undefined) updateData.nfc_terminal_enabled = body.nfcTerminalEnabled
    if (body.language !== undefined) updateData.language = body.language

    // Update user profile
    const { data, error } = await supabaseClient
      .from('user_profiles')
      .update({
        ...updateData,
        updated_at: new Date().toISOString()
      })
      .eq('id', body.userId)
      .select()
      .single()

    if (error) {
      console.error('Failed to update profile:', error)
      return new Response(
        JSON.stringify({
          success: false,
          error: error.message,
          code: 'UPDATE_FAILED'
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 500
        }
      )
    }

    console.log(`Profile updated successfully for user: ${body.userId}`)

    return new Response(
      JSON.stringify({
        success: true,
        message: 'Profile updated successfully'
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200
      }
    )

  } catch (error) {
    console.error('Update profile error:', error)
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
