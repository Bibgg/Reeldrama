'use strict';

// ReelDrama now uses the licensed/provider catalogue integration.
// Never insert demo titles or placeholder MP4s into the real catalogue.
// This module remains as a safe compatibility stub for old npm scripts and
// AUTO_SEED deployments. It intentionally performs no database writes.

async function populate() {
  console.warn('[seed] Disabled: demo/sample content is not allowed in ReelDrama.');
  return false;
}

async function seedIfEmpty() {
  console.warn('[seed] Disabled: provider catalogue only; no sample data inserted.');
  return false;
}

async function runStandalone() {
  console.log('No seed operation performed. ReelDrama does not use sample content.');
}

if (require.main === module) {
  runStandalone().catch((error) => {
    console.error('Seed compatibility command failed:', error.message);
    process.exitCode = 1;
  });
}

module.exports = { populate, seedIfEmpty };
