#!/usr/bin/env node

/**
 * Build script for AtlasStream Nuvio Providers
 * Bundles each provider from src/<provider>/index.js into providers/<provider>.js
 */

const esbuild = require('esbuild');
const fs = require('fs');
const path = require('path');

const srcDir = path.join(__dirname, 'src');
const outDir = path.join(__dirname, 'providers');

const EXTERNAL_MODULES = [
    'cheerio-without-node-native',
    'react-native-cheerio',
    'cheerio',
    'crypto-js',
    'axios'
];

function getProvidersToBuild() {
    const args = process.argv.slice(2).filter(arg => !arg.startsWith('-'));

    if (args.length > 0) {
        return args;
    }

    if (!fs.existsSync(srcDir)) {
        console.error('❌ src/ directory not found. Create provider folders in src/<provider>/');
        process.exit(1);
    }

    return fs.readdirSync(srcDir, { withFileTypes: true })
        .filter(d => d.isDirectory() && d.name !== 'shared')
        .map(d => d.name);
}

async function buildProvider(providerName) {
    const providerDir = path.join(srcDir, providerName);
    const entryPoint = path.join(providerDir, 'index.js');
    const outFile = path.join(outDir, `${providerName}.js`);

    if (!fs.existsSync(entryPoint)) {
        console.warn(`⚠️  Skipping ${providerName}: no src/${providerName}/index.js found`);
        return false;
    }

    try {
        await esbuild.build({
            entryPoints: [entryPoint],
            bundle: true,
            outfile: outFile,
            format: 'cjs',
            platform: 'neutral',
            target: ['es2020'],
            external: EXTERNAL_MODULES,
            minify: false,
            banner: {
                js: `/**\n * AtlasStream Nuvio Provider: ${providerName}\n * Generated: ${new Date().toISOString()}\n */`
            }
        });
        console.log(`✅ Built: providers/${providerName}.js`);
        return true;
    } catch (e) {
        console.error(`❌ Failed to build ${providerName}:`, e.message);
        return false;
    }
}

async function main() {
    if (!fs.existsSync(outDir)) {
        fs.mkdirSync(outDir, { recursive: true });
    }

    const providers = getProvidersToBuild();
    console.log(`🚀 Building ${providers.length} provider(s): ${providers.join(', ')}`);

    let success = 0;
    for (const p of providers) {
        if (await buildProvider(p)) success++;
    }

    console.log(`\n✨ Build complete: ${success}/${providers.length} successful.`);
}

main();
