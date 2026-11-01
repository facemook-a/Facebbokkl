"use strict";
const {createHash,timingSafeEqual}=require("node:crypto");
const hash=value=>createHash("sha256").update(value).digest("hex");
const millis=value=>value?.toMillis?.() ?? (typeof value==="number"?value:NaN);
function licenseExpiry(license) {
  const explicit=millis(license?.expires_at);
  if(Number.isFinite(explicit))return explicit;
  const anchor=millis(license?.activation_policy==="generation"?license?.created_at:license?.activated_at);
  return anchor + Number(license?.duration_days)*86400000;
}
function authorize({uid,binding,license,provider,settings,locationId,now=Date.now()}) {
  if(settings?.maintenance===true)throw Error("SERVICE_PAUSED");
  const expiresAt=licenseExpiry(license);
  if(!uid||!binding?.license_id||license?.schema_version!==3||license?.device_binding_id!==uid||license?.is_active!==true||license?.status!=="active"||!Number.isFinite(expiresAt)||expiresAt<=now)throw Error("LICENSE_INACTIVE");
  if(provider?.provider!=="aura"||provider.integration_verified!==true||provider.expiry_enforcement_verified!==true)throw Error("PROVIDER_NOT_CONNECTED");
  if(!provider.enabled_locations?.includes(locationId)||!provider.supported_us_location_ids?.includes(locationId))throw Error("LOCATION_DISABLED");
  return {expiresAt,grantExpiresAt:Math.min(expiresAt,now+60000)};
}
function secretMatches(received,expected) {
  if(typeof received!=="string"||typeof expected!=="string"||!expected)return false;
  return timingSafeEqual(Buffer.from(hash(received)),Buffer.from(hash(expected)));
}
module.exports={hash,millis,licenseExpiry,authorize,secretMatches};
