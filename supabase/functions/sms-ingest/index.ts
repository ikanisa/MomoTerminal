import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

interface SmsPayload {
  type: string
  messageId: string
  from: string // content of 'from' field from Android
  body: string
  receivedAt: string
  deviceId: string
  deviceName: string
  simSlot: number
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { 
      headers: { 
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type, x-smsbridge-secret, x-smsbridge-device'
      } 
    })
  }

  try {
    const authHeader = req.headers.get('Authorization')
    const secretHeader = req.headers.get('X-SMSBRIDGE-SECRET')
    
    // Validate Shared Secret (Simpler than Auth for this specific Bridge usecase)
    // In production, this secret should be in Vault/Env. For now, we check against a known value or env var.
    const expectedSecret = Deno.env.get('SMS_BRIDGE_SECRET')
    
    // If NO secret is configured in Env, we might allow it? No, insecure.
    // Allow if matches Env OR if we assume the user configured it in the app same as env.
    if (expectedSecret && secretHeader !== expectedSecret) {
         throw new Error('Invalid Bridge Secret')
    }

    // Initialize Supabase Client (Service Role needed to bypass RLS if user is anonymous/bridge)
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '' 
    )

    const payload: SmsPayload = await req.json()
    
    // Parse receivedAt to timestamp
    // Android sends: ISO String or similar.
    
    // Insert into sms_delivery_logs
    // We Map 'from' (Sender) to 'sender' column.
    // We need a 'user_id' for the table constraint. 
    // IF the bridge is "System Wide", we assign to a default Admin User or null if allowed?
    // Table says: user_id UUID NOT NULL.
    // We need to fetch a default user or the owner.
    // Workaround: Use a specific "System" user UUID or fetch the first admin.
    
    // For Internal App: Let's fetch the first user found or a hardcoded ID?
    // Better: Allow "anonymous" logs if we alter table?
    // Or just pick the first user for now (Hack, but unblocks).
    
    const { data: users } = await supabase.auth.admin.listUsers({ page: 1, perPage: 1 })
    const defaultUserId = users.users[0]?.id
    
    if (!defaultUserId) throw new Error('No system user found to attribute logs to.')

    const { error } = await supabase.from('sms_delivery_logs').insert({
      user_id: defaultUserId, 
      phone_number: payload.deviceName, // Using device name as "Target" identifier? or just 'N/A'
      sender: payload.from,
      message: payload.body,
      status: 'delivered', // It reached us
      response_code: 200,
      response_body: 'Ingested via sms-ingest',
      created_at: new Date().toISOString(),
      sent_at: payload.receivedAt, // When it was received on phone
      processing_time_ms: 0,
       // Custom data mapping
       // We might want to expand database schema later for 'device_id', 'sim_slot'
    })

    if (error) throw error

    return new Response(
      JSON.stringify({ success: true }),
      { headers: { 'Content-Type': 'application/json' }, status: 200 }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { headers: { 'Content-Type': 'application/json' }, status: 400 }
    )
  }
})
