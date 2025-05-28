const { override, babelInclude, addWebpackPlugin } = require('customize-cra');
const path = require('path');

module.exports = override(
  babelInclude([
    path.resolve('src'), // Include your app's source
    // Add other paths here if you have source code outside of 'src' that needs transpilation
  ]),
  // If you need to exclude specific modules from babel-loader (e.g., they are already transpiled)
  (config) => {
    // Find the babel-loader rule
    const babelLoaderRule = config.module.rules.find(rule =>
      rule.oneOf && rule.oneOf.find(oneOfRule => String(oneOfRule.loader).includes('babel-loader'))
    );

    if (babelLoaderRule) {
      const babelLoader = babelLoaderRule.oneOf.find(oneOfRule => String(oneOfRule.loader).includes('babel-loader'));
      if (babelLoader && babelLoader.include) {
        // Add exclusions for autolinker and react-zoom-pan-pinch
        // This assumes these modules are in node_modules and should not be processed by Babel
        const newExclude = [
          ...(babelLoader.exclude || []), // Keep existing excludes if any
          path.resolve('node_modules/autolinker'),
          path.resolve('node_modules/react-zoom-pan-pinch'),
        ];
        babelLoader.exclude = newExclude;

        // Make sure node_modules are generally processed if not already
        // This is tricky as create-react-app's default babel-loader setup is specific.
        // The babelInclude above is generally the better way to ensure your code is processed.
        // If you specifically want to include *some* node_modules for transpilation,
        // you'd modify the `include` array or use a more complex rule.
      }
    }
    return config;
  }
  // You can add other webpack plugins or customizations here if needed
  // For example, to add a plugin:
  // addWebpackPlugin(new YourWebpackPlugin())
);

// Fallback for older customize-cra versions or different setups
// module.exports = function override(config, env) {
//   // Ensure your src files are processed by Babel
//   config.module.rules[1].oneOf.forEach(rule => {
//     if (rule.loader && rule.loader.includes('babel-loader')) {
//       rule.include = path.resolve(__dirname, 'src');
//       rule.exclude = [
//         ...(rule.exclude || []),
//         path.resolve(__dirname, 'node_modules/autolinker'),
//         path.resolve(__dirname, 'node_modules/react-zoom-pan-pinch')
//       ];
//     }
//   });

//   // If you need to transpile specific node_modules (not recommended for these two)
//   // config.module.rules.push({
//   //   test: /\.m?js$/,
//   //   include: [
//   //     path.resolve(__dirname, "node_modules/some-other-module-to-transpile")
//   //   ],
//   //   use: {
//   //     loader: 'babel-loader',
//   //     options: {
//   //       presets: ['@babel/preset-env', '@babel/preset-react']
//   //     }
//   //   }
//   // });

//   return config;
// }; 