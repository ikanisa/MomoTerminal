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

    const { data, error } = await supabaseClient
      .from('vending_orders')
      .select(`
        *,
        vending_sessions!vending_sessions_order_id_fkey (*)
      `)
      .eq('user_id', user.id)
      .order('created_at', { ascending: false })

    if (error) throw error

    const orders = data.map(order => ({
      id: order.id,
      user_id: order.user_id,
      machine_id: order.machine_id,
      machine_name: order.machine_name,
      machine_location: order.machine_location,
      product_name: order.product_name,
      product_category: order.product_category,
      quantity: order.quantity,
      serving_size_ml: order.serving_size_ml,
      price_per_serving: order.price_per_serving,
      total_amount: order.total_amount,
      status: order.status,
      created_at: order.created_at,
      code: order.vending_sessions ? {
        code: '****',
        expires_at: order.vending_sessions.expires_at,
        total_serves: order.vending_sessions.total_serves,
        remaining_serves: order.vending_sessions.remaining_serves,
        used_at: order.vending_sessions.used_at,
        closed_at: order.vending_sessions.closed_at
      } : null
    }))

    return new Response(JSON.stringify({ orders }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: error.message === 'Unauthorized' ? 401 : 400,
    })
  }
})
