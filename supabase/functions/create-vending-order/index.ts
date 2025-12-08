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
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
    )

    const authHeader = req.headers.get('Authorization')!
    const token = authHeader.replace('Bearer ', '')
    const { data: { user } } = await supabaseClient.auth.getUser(token)

    if (!user) {
      throw new Error('Unauthorized')
    }

    const { machine_id, quantity } = await req.json()

    if (!machine_id || !quantity) {
      throw new Error('machine_id and quantity are required')
    }

    if (quantity < 1 || quantity > 10) {
      throw new Error('Quantity must be between 1 and 10 cups')
    }

    const { data, error } = await supabaseClient.rpc('create_vending_order', {
      p_user_id: user.id,
      p_machine_id: machine_id,
      p_quantity: quantity
    })

    if (error) throw error

    if (!data || data.length === 0) {
      throw new Error('Failed to create order')
    }

    const result = data[0]

    return new Response(
      JSON.stringify({
        order_id: result.order_id,
        order_status: result.order_status,
        code: result.code,
        code_expires_at: result.code_expires_at,
        total_serves: result.total_serves,
        remaining_serves: result.remaining_serves,
        wallet_balance: result.wallet_balance
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 201,
      },
    )
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: error.message === 'Unauthorized' ? 401 : 400,
      },
    )
  }
})
