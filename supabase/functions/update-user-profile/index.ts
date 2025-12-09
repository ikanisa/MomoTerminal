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

// Input sanitization helper
function sanitizeInput(input: string | undefined, maxLength: number = 255): string | undefined {
  if (!input) return undefined
  // Remove any non-printable characters, trim, and limit length
  return input.replace(/[^\x20-\x7E]/g, '').trim().slice(0, maxLength)
}

function validatePhoneNumber(phone: string | undefined): boolean {
  if (!phone) return true
  // Phone should be digits only, 8-15 characters
  return /^\d{8,15}$/.test(phone)
}

function validateCountryCode(code: string | undefined): boolean {
  if (!code) return true
  // Country code should be 2 uppercase letters
  return /^[A-Z]{2}$/.test(code)
}

function validateLanguage(lang: string | undefined): boolean {
  if (!lang) return true
  // Language code should be 2 lowercase letters
  return /^[a-z]{2}$/.test(lang)
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

    // Input validation and sanitization
    if (body.momoPhone && !validatePhoneNumber(body.momoPhone)) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Invalid phone number format',
          code: 'INVALID_PHONE'
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400
        }
      )
    }

    if (body.countryCode && !validateCountryCode(body.countryCode)) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Invalid country code format',
          code: 'INVALID_COUNTRY_CODE'
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400
        }
      )
    }

    if (body.momoCountryCode && !validateCountryCode(body.momoCountryCode)) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Invalid MoMo country code format',
          code: 'INVALID_MOMO_COUNTRY_CODE'
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400
        }
      )
    }

    if (body.language && !validateLanguage(body.language)) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'Invalid language code format',
          code: 'INVALID_LANGUAGE'
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400
        }
      )
    }

    // Build update object with only provided fields (sanitized)
    const updateData: any = {}
    if (body.countryCode !== undefined) updateData.country_code = sanitizeInput(body.countryCode, 2)
    if (body.momoCountryCode !== undefined) updateData.momo_country_code = sanitizeInput(body.momoCountryCode, 2)
    if (body.momoPhone !== undefined) updateData.momo_phone = sanitizeInput(body.momoPhone, 15)
    if (body.useMomoCode !== undefined) updateData.use_momo_code = body.useMomoCode
    if (body.merchantName !== undefined) updateData.merchant_name = sanitizeInput(body.merchantName, 100)
    if (body.biometricEnabled !== undefined) updateData.biometric_enabled = body.biometricEnabled
    if (body.nfcTerminalEnabled !== undefined) updateData.nfc_terminal_enabled = body.nfcTerminalEnabled
    if (body.language !== undefined) updateData.language = sanitizeInput(body.language, 2)

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
