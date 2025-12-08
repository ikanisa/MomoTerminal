import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: req.headers.get('Authorization')! },
        },
      }
    )

    const { data: { user } } = await supabaseClient.auth.getUser()
    if (!user) throw new Error('Unauthorized')

    const url = new URL(req.url)
    const orderId = url.pathname.split('/').pop()

    const { data, error } = await supabaseClient
      .from('vending_orders')
      .select(`
        *,
        vending_sessions!vending_sessions_order_id_fkey (*)
      `)
      .eq('id', orderId)
      .eq('user_id', user.id)
      .single()

    if (error) throw error

    const order = {
      id: data.id,
      user_id: data.user_id,
      machine_id: data.machine_id,
      machine_name: data.machine_name,
      machine_location: data.machine_location,
      product_name: data.product_name,
      product_category: data.product_category,
      quantity: data.quantity,
      serving_size_ml: data.serving_size_ml,
      price_per_serving: data.price_per_serving,
      total_amount: data.total_amount,
      status: data.status,
      created_at: data.created_at,
      code: data.vending_sessions ? {
        code: data.vending_sessions.code_plain || '****',
        expires_at: data.vending_sessions.expires_at,
        total_serves: data.vending_sessions.total_serves,
        remaining_serves: data.vending_sessions.remaining_serves,
        used_at: data.vending_sessions.used_at,
        closed_at: data.vending_sessions.closed_at
      } : null
    }

    return new Response(JSON.stringify(order), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: error.message === 'Unauthorized' ? 401 : 404,
    })
  }
})
