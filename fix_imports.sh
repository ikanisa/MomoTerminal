#!/bin/bash

# Fix imports across all modules
echo "Fixing imports in core modules..."

# Fix imports in core modules
find core/*/src/main/kotlin -name "*.kt" -exec sed -i '' \
  -e 's/import com\.momoterminal\.util\.Result/import com.momoterminal.core.common.Result/g' \
  -e 's/import com\.momoterminal\.util\./import com.momoterminal.core.common./g' \
  -e 's/import com\.momoterminal\.api\./import com.momoterminal.core.network.api./g' \
  -e 's/import com\.momoterminal\.supabase\./import com.momoterminal.core.network.supabase./g' \
  -e 's/import com\.momoterminal\.data\.local\./import com.momoterminal.core.database./g' \
  -e 's/import com\.momoterminal\.data\.repository\./import com.momoterminal.core.data.repository./g' \
  -e 's/import com\.momoterminal\.data\.mapper\./import com.momoterminal.core.data.mapper./g' \
  -e 's/import com\.momoterminal\.domain\.model\./import com.momoterminal.core.domain.model./g' \
  -e 's/import com\.momoterminal\.domain\.repository\./import com.momoterminal.core.domain.repository./g' \
  -e 's/import com\.momoterminal\.domain\.usecase\./import com.momoterminal.core.domain.usecase./g' \
  -e 's/import com\.momoterminal\.designsystem\./import com.momoterminal.core.designsystem./g' \
  {} \;

echo "Fixing imports in feature modules..."

# Fix imports in feature modules
find feature/*/src/main/kotlin -name "*.kt" -exec sed -i '' \
  -e 's/import com\.momoterminal\.util\.Result/import com.momoterminal.core.common.Result/g' \
  -e 's/import com\.momoterminal\.util\./import com.momoterminal.core.common./g' \
  -e 's/import com\.momoterminal\.api\./import com.momoterminal.core.network.api./g' \
  -e 's/import com\.momoterminal\.supabase\./import com.momoterminal.core.network.supabase./g' \
  -e 's/import com\.momoterminal\.data\.local\./import com.momoterminal.core.database./g' \
  -e 's/import com\.momoterminal\.data\.repository\./import com.momoterminal.core.data.repository./g' \
  -e 's/import com\.momoterminal\.domain\.model\./import com.momoterminal.core.domain.model./g' \
  -e 's/import com\.momoterminal\.domain\.repository\./import com.momoterminal.core.domain.repository./g' \
  -e 's/import com\.momoterminal\.designsystem\./import com.momoterminal.core.designsystem./g' \
  -e 's/import com\.momoterminal\.nfc\./import com.momoterminal.feature.payment.nfc./g' \
  -e 's/import com\.momoterminal\.ussd\./import com.momoterminal.feature.payment.ussd./g' \
  -e 's/import com\.momoterminal\.auth\./import com.momoterminal.feature.auth./g' \
  {} \;

echo "Fixing imports in app module..."

# Fix imports in app module
find app/src/main/java -name "*.kt" -exec sed -i '' \
  -e 's/import com\.momoterminal\.util\.Result/import com.momoterminal.core.common.Result/g' \
  -e 's/import com\.momoterminal\.util\.Extensions/import com.momoterminal.core.common.Extensions/g' \
  -e 's/import com\.momoterminal\.util\.Constants/import com.momoterminal.core.common.Constants/g' \
  -e 's/import com\.momoterminal\.designsystem\./import com.momoterminal.core.designsystem./g' \
  {} \;

echo "Import fixes complete!"
