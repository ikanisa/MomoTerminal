// Follow this setup guide to integrate the Deno runtime into your app:
// https://deno.land/manual/getting_started/setup_your_environment

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const WHATSAPP_API_URL = "https://graph.facebook.com/v18.0"

// CORS configuration
const CORS_HEADERS = {
  'Access-Control-Allow-Origin': '*', // TODO: Restrict to your domain in production
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, apikey, x-client-info',
  'Access-Control-Max-Age': '86400',
}

// Rate limiting configuration
const RATE_LIMITS = {
  PER_PHONE_10MIN: 5,      // Max 5 OTPs per phone per 10 minutes
  PER_IP_HOUR: 50,         // Max 50 OTPs per IP per hour
  GLOBAL_MINUTE: 100,      // Max 100 OTPs globally per minute
}

interface RequestBody {
  phoneNumber: string
}

interface WhatsAppMessageResponse {
  messaging_product: string
  contacts: Array<{ input: string; wa_id: string }>
  messages: Array<{ id: string }>
}

// Helper to extract client IP
function getClientIP(req: Request): string {
  return req.headers.get('x-forwarded-for')?.split(',')[0]?.trim() 
    || req.headers.get('x-real-ip')
    || 'unknown'
}

// Calculate exponential backoff delay
function getBackoffDelay(attempts: number): number {
  // 1st retry: 1s, 2nd: 2s, 3rd: 4s, 4th: 8s, 5th: 16s
  return Math.min(Math.pow(2, attempts - 1) * 1000, 60000) // Max 60s
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

    // Get client IP for rate limiting
    const clientIP = getClientIP(req)
    console.log(`OTP request from IP: ${clientIP} for phone: ${phoneNumber}`)

    // Validate phone number format (E.164)
    const phoneRegex = /^\+[1-9]\d{9,14}$/
    if (!phoneNumber || !phoneRegex.test(phoneNumber)) {
      return new Response(
        JSON.stringify({ 
          error: 'Invalid phone number format. Must be E.164 format (e.g., +250788767816)',
          code: 'INVALID_PHONE_FORMAT'
        }),
        { status: 400, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
    }

    // Create Supabase client with service role key
    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

    // === MULTI-LAYER RATE LIMITING ===
    
    // 1. Per-phone rate limit (5 per 10 minutes)
    const tenMinutesAgo = new Date(Date.now() - 10 * 60 * 1000).toISOString()
    const { count: recentPhoneOtpCount } = await supabase
      .from('otp_codes')
      .select('*', { count: 'exact', head: true })
      .eq('phone_number', phoneNumber)
      .gte('created_at', tenMinutesAgo)

    if (recentPhoneOtpCount && recentPhoneOtpCount >= RATE_LIMITS.PER_PHONE_10MIN) {
      const retryAfter = getBackoffDelay(recentPhoneOtpCount - RATE_LIMITS.PER_PHONE_10MIN + 1)
      console.log(`Rate limit exceeded for phone ${phoneNumber}: ${recentPhoneOtpCount} requests`)
      
      return new Response(
        JSON.stringify({ 
          error: 'Too many OTP requests. Please wait before trying again.',
          code: 'RATE_LIMIT_PHONE',
          retryAfter: Math.ceil(retryAfter / 1000)
        }),
        { 
          status: 429, 
          headers: { 
            ...CORS_HEADERS,
            'Content-Type': 'application/json',
            'Retry-After': String(Math.ceil(retryAfter / 1000))
          } 
        }
      )
    }

    // 2. Per-IP rate limit (50 per hour)
    if (clientIP !== 'unknown') {
      const oneHourAgo = new Date(Date.now() - 60 * 60 * 1000).toISOString()
      const { count: recentIPCount } = await supabase
        .from('otp_request_logs')
        .select('*', { count: 'exact', head: true })
        .eq('ip_address', clientIP)
        .gte('created_at', oneHourAgo)

      if (recentIPCount && recentIPCount >= RATE_LIMITS.PER_IP_HOUR) {
        console.log(`Rate limit exceeded for IP ${clientIP}: ${recentIPCount} requests`)
        
        return new Response(
          JSON.stringify({ 
            error: 'Too many requests from this location. Please try again later.',
            code: 'RATE_LIMIT_IP',
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

    // 3. Global rate limit (100 per minute - abuse prevention)
    const oneMinuteAgo = new Date(Date.now() - 60 * 1000).toISOString()
    const { count: globalRecentCount } = await supabase
      .from('otp_codes')
      .select('*', { count: 'exact', head: true })
      .gte('created_at', oneMinuteAgo)

    if (globalRecentCount && globalRecentCount >= RATE_LIMITS.GLOBAL_MINUTE) {
      console.warn(`Global rate limit exceeded: ${globalRecentCount} requests in last minute`)
      
      return new Response(
        JSON.stringify({ 
          error: 'Service temporarily busy. Please try again in a moment.',
          code: 'RATE_LIMIT_GLOBAL',
          retryAfter: 60
        }),
        { 
          status: 429, 
          headers: { 
            ...CORS_HEADERS,
            'Content-Type': 'application/json',
            'Retry-After': '60'
          } 
        }
      )
    }

    // Log the request for IP-based rate limiting and analytics
    await supabase
      .from('otp_request_logs')
      .insert({
        phone_number: phoneNumber,
        ip_address: clientIP,
        user_agent: req.headers.get('user-agent') || 'unknown',
        request_type: 'send_otp'
      })
      .catch(e => console.error('Failed to log request:', e))

    // Generate 6-digit OTP using cryptographically secure random
    const array = new Uint32Array(1)
    crypto.getRandomValues(array)
    const otpCode = String(100000 + (array[0] % 900000)).padStart(6, '0')

    // Hash the OTP for secure storage (SHA-256 with phone number as salt)
    const encoder = new TextEncoder()
    const data = encoder.encode(otpCode + phoneNumber)
    const hashBuffer = await crypto.subtle.digest('SHA-256', data)
    const hashArray = Array.from(new Uint8Array(hashBuffer))
    const otpHash = hashArray.map(b => b.toString(16).padStart(2, '0')).join('')

    // Set expiry time (5 minutes from now)
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString()

    // Save hashed OTP to database (never store plaintext)
    const { data: otpData, error: dbError } = await supabase
      .from('otp_codes')
      .insert({
        phone_number: phoneNumber,
        code: otpHash, // Store hash instead of plaintext
        template_name: 'momo_terminal',
        channel: 'whatsapp',
        expires_at: expiresAt,
        attempts: 0
      })
      .select()
      .single()

    if (dbError) {
      console.error('Database error:', dbError)
      return new Response(
        JSON.stringify({ 
          error: 'Unable to process request. Please try again.',
          code: 'DB_ERROR'
        }),
        { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
      )
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
          code: 'DELIVERY_FAILED'
        }),
        { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
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
        headers: { 'Content-Type': 'application/json' } 
      }
    )

  } catch (error) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ 
        error: error instanceof Error ? error.message : 'Internal server error' 
      }),
      { status: 500, headers: { ...CORS_HEADERS, 'Content-Type': 'application/json' } }
    )
  }
})
