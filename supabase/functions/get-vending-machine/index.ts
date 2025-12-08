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
    const machineId = url.pathname.split('/').pop()

    const { data, error } = await supabaseClient
      .from('vending_machines')
      .select(`
        *,
        vending_products!vending_machines_product_id_fkey (*)
      `)
      .eq('id', machineId)
      .single()

    if (error) throw error

    const machine = {
      id: data.id,
      name: data.name,
      location: data.location,
      latitude: data.latitude,
      longitude: data.longitude,
      status: data.status,
      product_id: data.product_id,
      product_name: data.vending_products.name,
      product_category: data.vending_products.category,
      serving_size_ml: data.vending_products.serving_size_ml,
      price_per_serving: data.vending_products.price_per_serving,
      currency: 'XAF',
      stock_level: data.stock_level,
      image_url: data.image_url,
      is_age_restricted: data.vending_products.is_age_restricted
    }

    return new Response(JSON.stringify(machine), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: error.message === 'Unauthorized' ? 401 : 404,
    })
  }
})
