// Supabase Edge Function: send-otp
// Deploy with: supabase functions deploy send-otp

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const { phone } = await req.json()
    
    if (!phone) {
      return new Response(
        JSON.stringify({ error: 'Phone number required' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    // Generate 6-digit OTP
    const otp = Math.floor(100000 + Math.random() * 900000).toString()
    
    // Store OTP in database with expiry (5 minutes)
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    
    // Store OTP (you'd typically use a separate otp_codes table)
    // For now, we'll use a simple in-memory approach or Redis
    
    // Send OTP via SMS (integrate with your SMS provider)
    // Options: Twilio, Africa's Talking, Pindo (Rwanda), etc.
    
    // For development, just log the OTP
    console.log(`OTP for ${phone}: ${otp}`)
    
    // TODO: Integrate with SMS provider
    // Example with Africa's Talking:
    // const response = await fetch('https://api.africastalking.com/version1/messaging', {
    //   method: 'POST',
    //   headers: {
    //     'apiKey': Deno.env.get('AT_API_KEY')!,
    //     'Content-Type': 'application/x-www-form-urlencoded',
    //   },
    //   body: `username=${Deno.env.get('AT_USERNAME')}&to=${phone}&message=Your MomoTerminal code is: ${otp}`
    // })

    return new Response(
      JSON.stringify({ 
        success: true, 
        message: 'OTP sent',
        // Remove in production - only for testing
        debug_otp: otp 
      }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
