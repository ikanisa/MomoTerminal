// Follow this setup guide to integrate the Deno runtime into your app:
// https://deno.land/manual/getting_started/setup_your_environment

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const WHATSAPP_API_URL = "https://graph.facebook.com/v18.0"

// CORS headers for cross-origin requests
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
}

// Input validation patterns
const phoneRegex = /^\+[1-9]\d{9,14}$/

interface RequestBody {
  phoneNumber: string
}

interface WhatsAppMessageResponse {
  messaging_product: string
  contacts: Array<{ input: string; wa_id: string }>
  messages: Array<{ id: string }>
}

// Helper function to generate cryptographically secure OTP
function generateSecureOTP(): string {
  const array = new Uint32Array(1)
  crypto.getRandomValues(array)
  return String(100000 + (array[0] % 900000)).padStart(6, '0')
}

// Helper function to hash OTP with SHA-256
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
    // Get environment variables from Supabase secrets
    const WHATSAPP_PHONE_NUMBER_ID = Deno.env.get('WA_PHONE_ID') || Deno.env.get('WHATSAPP_PHONE_NUMBER_ID')
    const WHATSAPP_ACCESS_TOKEN = Deno.env.get('WA_TOKEN') || Deno.env.get('WHATSAPP_ACCESS_TOKEN')
    const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!
    const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!

    if (!WHATSAPP_PHONE_NUMBER_ID || !WHATSAPP_ACCESS_TOKEN) {
      throw new Error('WhatsApp credentials not configured')
    }

    // Parse request body
    const { phoneNumber }: RequestBody = await req.json()

    if (!phoneNumber) {
      return new Response(
        JSON.stringify({ error: 'Phone number is required', code: 'MISSING_PHONE' }),
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

    // Create Supabase client with service role key
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

    // Check rate limiting (max 5 OTPs per 10 minutes per phone)
    const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString()
    const { count: recentOtpCount } = await supabase
      .from('otp_codes')
      .select('*', { count: 'exact', head: true })
      .eq('phone_number', phoneNumber)
      .gte('created_at', tenMinutesAgo)

    if (recentOtpCount && recentOtpCount >= 5) {
      return new Response(
        JSON.stringify({ 
          error: 'Too many OTP requests. Please try again later.',
          code: 'RATE_LIMITED',
          retryAfter: 600 
        }),
        { status: 429, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Generate cryptographically secure 6-digit OTP
    const otpCode = generateSecureOTP()

    // Hash OTP before storage (salted with phone number)
    const hashedOtp = await hashOTP(otpCode, phoneNumber)

    // Set expiry time (5 minutes from now)
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString()

    // Save hashed OTP to database
    const { data: otpData, error: dbError } = await supabase
      .from('otp_codes')
      .insert({
        phone_number: phoneNumber,
        code: hashedOtp,
        template_name: 'momo_terminal',
        channel: 'whatsapp',
        expires_at: expiresAt,
        attempts: 0
      })
      .select()
      .single()

    if (dbError) {
      console.error('Database error:', dbError)
      throw new Error('Failed to save OTP')
    }

    // Send WhatsApp message using Meta Business API
    const whatsappUrl = `${WHATSAPP_API_URL}/${WHATSAPP_PHONE_NUMBER_ID}/messages`
    
    const whatsappPayload = {
      messaging_product: "whatsapp",
      to: phoneNumber,
      type: "template",
      template: {
        name: "momo_terminal",
        language: {
          code: "en"
        },
        components: [
          {
            type: "body",
            parameters: [
              {
                type: "text",
                text: otpCode
              }
            ]
          },
          {
            type: "button",
            sub_type: "url",
            index: 0,
            parameters: [
              {
                type: "text",
                text: otpCode
              }
            ]
          }
        ]
      }
    }

    console.log('Sending WhatsApp OTP to:', phoneNumber)

    const whatsappResponse = await fetch(whatsappUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${WHATSAPP_ACCESS_TOKEN}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(whatsappPayload)
    })

    const whatsappData: WhatsAppMessageResponse = await whatsappResponse.json()

    if (!whatsappResponse.ok) {
      console.error('WhatsApp API error:', JSON.stringify(whatsappData))
      
      // Delete the OTP from database since sending failed
      await supabase
        .from('otp_codes')
        .delete()
        .eq('id', otpData.id)

      return new Response(
        JSON.stringify({ 
          error: 'Unable to send OTP. Please try again.',
          code: 'SEND_FAILED'
        }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Update OTP record with message ID
    if (whatsappData.messages && whatsappData.messages.length > 0) {
      await supabase
        .from('otp_codes')
        .update({ message_id: whatsappData.messages[0].id })
        .eq('id', otpData.id)
    }

    console.log('WhatsApp OTP sent successfully:', whatsappData)

    return new Response(
      JSON.stringify({
        success: true,
        message: 'OTP sent successfully',
        expiresAt: expiresAt,
        expiresInSeconds: 300
      }),
      { 
        status: 200, 
        headers: { ...corsHeaders, 'Content-Type': 'application/json' } 
      }
    )

  } catch (error) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ 
        error: 'An error occurred. Please try again.',
        code: 'INTERNAL_ERROR'
      }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
