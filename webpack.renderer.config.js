const config = require('./webpack.base.config');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const path = require('path');

const rendererConfig = { ...config };
rendererConfig.target = 'electron-renderer';
rendererConfig.entry = {
  'renderer': './src/renderer/renderer.ts',
  'preload': './src/browser/preload.ts',
};

rendererConfig.plugins.push(new HtmlWebpackPlugin({
  template: './src/renderer/index.html',
  filename: path.join(__dirname, './dist/renderer/index.html'),
  chunks: ['renderer'],
  publicPath: '',
  inject: false
}));

module.exports = rendererConfig;