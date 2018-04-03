const {injectBabelPlugin} = require('react-app-rewired');
const SWPrecacheWebpackPlugin = require('sw-precache-webpack-plugin');

module.exports = function override(config, env) {
  config = injectBabelPlugin(['import', {libraryName: 'antd', style: 'css'}], config);

  let swPrecachePlugin = config.plugins.find(p => p instanceof SWPrecacheWebpackPlugin);

  // Only during production build:
  if (swPrecachePlugin) {
    swPrecachePlugin.options.runtimeCaching = [
      {
        urlPattern: /\/api\//,
        handler: 'fastest'
      }
    ];
  }

  return config;
};
