import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface RegisterDeviceRequest {
  device_id: string
  device_name: string
  device_model: string
  manufacturer: string
  os_version: string
  sdk_version: number
  app_version: string
  fcm_token?: string
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: 'Missing authorization header' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    )

    const { data: { user }, error: userError } = await supabaseClient.auth.getUser()

    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: 'Unauthorized' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const request: RegisterDeviceRequest = await req.json()

    if (!request.device_id || !request.device_name || !request.device_model) {
      return new Response(
        JSON.stringify({ error: 'Missing required fields' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const { data: device, error: deviceError } = await supabaseClient
      .from('devices')
      .upsert({
        user_id: user.id,
        device_id: request.device_id,
        device_name: request.device_name,
        device_model: request.device_model,
        manufacturer: request.manufacturer,
        os_version: request.os_version,
        sdk_version: request.sdk_version,
        app_version: request.app_version,
        fcm_token: request.fcm_token,
        fcm_token_updated_at: request.fcm_token ? new Date().toISOString() : null,
        last_active_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }, { onConflict: 'user_id,device_id' })
      .select()
      .single()

    if (deviceError) {
      console.error('Device registration error:', deviceError)
      return new Response(
        JSON.stringify({ error: 'Failed to register device' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    return new Response(
      JSON.stringify({
        id: device.id,
        device_id: device.device_id,
        is_trusted: device.is_trusted,
        registered_at: device.registered_at,
      }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  } catch (error) {
    console.error('Error:', error)
    return new Response(
      JSON.stringify({ error: 'Internal server error' }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})
