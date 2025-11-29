# send-whatsapp-otp Edge Function

## Overview
This Supabase Edge Function sends a WhatsApp OTP to a user's phone number for authentication.

## Endpoint
`POST /functions/v1/send-whatsapp-otp`

## Request Headers
```
Authorization: Bearer <SUPABASE_ANON_KEY>
Content-Type: application/json
```

## Request Body
```json
{
  "phone_number": "+250788767816",
  "template_name": "momo_terminal"
}
```

### Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| phone_number | string | Yes | E.164 formatted phone number (e.g., +250788767816) |
| template_name | string | No | WhatsApp template name (default: "momo_terminal") |

## Response

### Success (200)
```json
{
  "success": true,
  "message_id": "wamid.HBgNMjU...",
  "expires_in_seconds": 300
}
```

### Rate Limited (429)
```json
{
  "success": false,
  "error": "rate_limit_exceeded",
  "message": "Too many OTP requests. Please wait before requesting again.",
  "retry_after_seconds": 60
}
```

### Error (400/500)
```json
{
  "success": false,
  "error": "invalid_phone",
  "message": "Invalid phone number format"
}
```

## Implementation Details

### Function Logic
1. Validate the phone number format (E.164)
2. Check rate limiting (max 5 OTPs per hour per phone)
3. Generate a secure 6-digit OTP
4. Store OTP in `otp_codes` table with 5-minute expiry
5. Send WhatsApp message using Business API with template
6. Return success with message ID and expiry time

### Environment Variables Required
| Variable | Description |
|----------|-------------|
| WHATSAPP_PHONE_NUMBER_ID | WhatsApp Business Phone Number ID |
| WHATSAPP_ACCESS_TOKEN | WhatsApp Business API Access Token |
| SUPABASE_SERVICE_ROLE_KEY | Supabase service role key for database access |

### Example Implementation (TypeScript/Deno)

```typescript
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const WHATSAPP_API_URL = 'https://graph.facebook.com/v18.0'
const OTP_EXPIRY_SECONDS = 300 // 5 minutes
const RATE_LIMIT_WINDOW_HOURS = 1
const RATE_LIMIT_MAX_REQUESTS = 5

serve(async (req) => {
  try {
    const { phone_number, template_name = 'momo_terminal' } = await req.json()
    
    // Validate phone number format (E.164)
    if (!phone_number?.match(/^\+[1-9]\d{1,14}$/)) {
      return new Response(
        JSON.stringify({ success: false, error: 'invalid_phone', message: 'Invalid phone number format' }),
        { status: 400, headers: { 'Content-Type': 'application/json' } }
      )
    }
    
    // Initialize Supabase client with service role
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    )
    
    // Check rate limiting
    const { data: recentOtps } = await supabase
      .from('otp_codes')
      .select('id')
      .eq('phone_number', phone_number)
      .gte('created_at', new Date(Date.now() - RATE_LIMIT_WINDOW_HOURS * 60 * 60 * 1000).toISOString())
    
    if (recentOtps && recentOtps.length >= RATE_LIMIT_MAX_REQUESTS) {
      return new Response(
        JSON.stringify({
          success: false,
          error: 'rate_limit_exceeded',
          message: 'Too many OTP requests. Please wait before requesting again.',
          retry_after_seconds: 60
        }),
        { status: 429, headers: { 'Content-Type': 'application/json' } }
      )
    }
    
    // Generate secure 6-digit OTP
    const code = String(Math.floor(100000 + Math.random() * 900000))
    const expiresAt = new Date(Date.now() + OTP_EXPIRY_SECONDS * 1000).toISOString()
    
    // Store OTP in database
    const { error: insertError } = await supabase
      .from('otp_codes')
      .insert({
        phone_number,
        code,
        template_name,
        expires_at: expiresAt
      })
    
    if (insertError) {
      console.error('Error storing OTP:', insertError)
      return new Response(
        JSON.stringify({ success: false, error: 'server_error', message: 'Failed to generate OTP' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }
    
    // Send WhatsApp message
    const whatsappResponse = await fetch(
      `${WHATSAPP_API_URL}/${Deno.env.get('WHATSAPP_PHONE_NUMBER_ID')}/messages`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${Deno.env.get('WHATSAPP_ACCESS_TOKEN')}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          messaging_product: 'whatsapp',
          to: phone_number.replace('+', ''),
          type: 'template',
          template: {
            name: template_name,
            language: { code: 'en' },
            components: [
              {
                type: 'body',
                parameters: [
                  { type: 'text', text: code }
                ]
              }
            ]
          }
        })
      }
    )
    
    const whatsappResult = await whatsappResponse.json()
    
    if (!whatsappResponse.ok) {
      console.error('WhatsApp API error:', whatsappResult)
      return new Response(
        JSON.stringify({ success: false, error: 'whatsapp_error', message: 'Failed to send WhatsApp message' }),
        { status: 500, headers: { 'Content-Type': 'application/json' } }
      )
    }
    
    return new Response(
      JSON.stringify({
        success: true,
        message_id: whatsappResult.messages?.[0]?.id,
        expires_in_seconds: OTP_EXPIRY_SECONDS
      }),
      { status: 200, headers: { 'Content-Type': 'application/json' } }
    )
    
  } catch (error) {
    console.error('Error in send-whatsapp-otp:', error)
    return new Response(
      JSON.stringify({ success: false, error: 'server_error', message: 'Internal server error' }),
      { status: 500, headers: { 'Content-Type': 'application/json' } }
    )
  }
})
```

## Security Considerations

1. **Never log OTP codes** - Only log message IDs and phone numbers (partially masked)
2. **Rate limiting** - Enforce max 5 OTPs per hour per phone number
3. **Secure random generation** - Use cryptographically secure random for OTP generation
4. **Short expiry** - OTPs expire after 5 minutes
5. **IP tracking** - Log IP addresses for abuse detection
6. **Template security** - Use pre-approved WhatsApp templates only

## WhatsApp Template Setup

The `momo_terminal` template should be configured in WhatsApp Business Manager with the following structure:

**Template Name:** `momo_terminal`
**Category:** Authentication
**Language:** English

**Header:** None
**Body:** Your MomoTerminal verification code is {{1}}. This code expires in 5 minutes. Do not share this code.
**Footer:** Powered by MomoTerminal

## Testing

```bash
# Test with curl
curl -X POST 'https://lhbowpbcpwoiparwnwgt.supabase.co/functions/v1/send-whatsapp-otp' \
  -H 'Authorization: Bearer <SUPABASE_ANON_KEY>' \
  -H 'Content-Type: application/json' \
  -d '{"phone_number": "+250788767816"}'
```
