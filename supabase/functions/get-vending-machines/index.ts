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
    const lat = url.searchParams.get('latitude')
    const lng = url.searchParams.get('longitude')
    const radius = url.searchParams.get('radius_km') || '10'

    const { data, error } = await supabaseClient
      .from('vending_machines')
      .select(`
        *,
        vending_products!vending_machines_product_id_fkey (*)
      `)
      .in('status', ['AVAILABLE', 'LOW_STOCK'])
      .neq('stock_level', 'OUT_OF_STOCK')

    if (error) throw error

    const machines = data.map(machine => ({
      id: machine.id,
      name: machine.name,
      location: machine.location,
      latitude: machine.latitude,
      longitude: machine.longitude,
      status: machine.status,
      product_id: machine.product_id,
      product_name: machine.vending_products.name,
      product_category: machine.vending_products.category,
      serving_size_ml: machine.vending_products.serving_size_ml,
      price_per_serving: machine.vending_products.price_per_serving,
      currency: 'XAF',
      stock_level: machine.stock_level,
      image_url: machine.image_url,
      is_age_restricted: machine.vending_products.is_age_restricted
    }))

    return new Response(JSON.stringify({ machines }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: error.message === 'Unauthorized' ? 401 : 400,
    })
  }
})
