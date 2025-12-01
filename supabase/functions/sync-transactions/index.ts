import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

interface Transaction {
  local_id: number
  sender: string
  body: string
  timestamp: string
  status: string
  amount?: number
  currency?: string
  transaction_id?: string
  merchant_code?: string
  provider?: string
  provider_type?: string
}

interface SyncRequest {
  transactions: Transaction[]
  device_id: string
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
    if (!authHeader) throw new Error('Missing authorization header')
    
    const token = authHeader.replace('Bearer ', '')
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    )
    
    const { data: { user }, error: authError } = await supabase.auth.getUser(token)
    if (authError || !user) throw new Error('Invalid authentication token')
    
    const { transactions, device_id }: SyncRequest = await req.json()
    
    const transactionsToInsert = transactions.map(t => ({
      user_id: user.id,
      device_id: device_id,
      local_id: t.local_id,
      sender: t.sender,
      body: t.body,
      timestamp: t.timestamp,
      status: t.status,
      amount: t.amount || null,
      currency: t.currency || 'GHS',
      transaction_id: t.transaction_id || null,
      merchant_code: t.merchant_code || null,
      provider: t.provider || null,
      provider_type: t.provider_type || null,
      synced_at: new Date().toISOString()
    }))
    
    const { data, error } = await supabase
      .from('transactions')
      .upsert(transactionsToInsert, {
        onConflict: 'user_id,local_id,device_id',
        ignoreDuplicates: false
      })
      .select('id')
    
    if (error) throw error
    
    await supabase.rpc('update_device_activity', {
      p_device_id: device_id,
      p_user_id: user.id,
      p_transaction_count: transactions.length
    })
    
    return new Response(
      JSON.stringify({ 
        success: true, 
        synced: data?.length || 0,
        message: `Successfully synced ${data?.length || 0} transactions`
      }),
      { 
        headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }, 
        status: 200 
      }
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ success: false, error: error.message }),
      { headers: { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' }, status: 400 }
    )
  }
})
