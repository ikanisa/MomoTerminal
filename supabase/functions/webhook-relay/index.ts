import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { createHmac } from "https://deno.land/std@0.160.0/node/crypto.ts"

interface WebhookRequest {
  webhook_id: string
  sms_data: {
    sender: string
    message: string
    timestamp: string
    phone_number: string
    parsed_data?: Record<string, any>
  }
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { 
      headers: { 
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type'
      } 
    })
  }

  try {
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) throw new Error('Missing authorization')
    
    const token = authHeader.replace('Bearer ', '')
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    )
    
    const { data: { user } } = await supabase.auth.getUser(token)
    if (!user) throw new Error('Unauthorized')
    
    const { webhook_id, sms_data }: WebhookRequest = await req.json()
    const { data: webhook } = await supabase
      .from('webhook_configs')
      .select('*')
      .eq('id', webhook_id)
      .eq('user_id', user.id)
      .single()
    
    if (!webhook || !webhook.is_active) throw new Error('Webhook not found or inactive')
    
    const payload = {
      phone_number: sms_data.phone_number,
      sender: sms_data.sender,
      message: sms_data.message,
      timestamp: sms_data.timestamp,
      ...sms_data.parsed_data
    }
    
    const payloadString = JSON.stringify(payload)
    const signature = createHmac('sha256', webhook.hmac_secret)
      .update(payloadString)
      .digest('hex')
    
    const startTime = Date.now()
    const response = await fetch(webhook.url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Webhook-Signature': signature,
        'X-Webhook-Timestamp': new Date().toISOString(),
        'Authorization': `Bearer ${webhook.api_key}`
      },
      body: payloadString
    })
    
    const processingTime = Date.now() - startTime
    const responseText = await response.text()
    
    await supabase.from('sms_delivery_logs').insert({
      user_id: user.id,
      webhook_id: webhook.id,
      phone_number: sms_data.phone_number,
      sender: sms_data.sender,
      message: sms_data.message,
      status: response.ok ? 'sent' : 'failed',
      response_code: response.status,
      response_body: responseText.substring(0, 1000),
      sent_at: response.ok ? new Date().toISOString() : null,
      processing_time_ms: processingTime
    })
    
    return new Response(
      JSON.stringify({ 
        success: response.ok,
        status: response.status,
        processing_time_ms: processingTime
      }),
      { headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }, status: 200 }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }, status: 400 }
    )
  }
})
