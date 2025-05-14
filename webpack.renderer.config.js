const config = require('./webpack.base.config');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const path = require('path');

const rendererConfig = { ...config };
rendererConfig.target = 'electron-renderer';
rendererConfig.entry = {
  renderer: './src/renderer/renderer.ts',
  preload: './src/preload/preload.ts',
  exclusive: './src/renderer/exclusive.ts'
};

const htmlPlugins = [
  new HtmlWebpackPlugin({
    template: './src/renderer/index.html',
    filename: path.join(__dirname, './dist/renderer/index.html'),
    chunks: ['renderer'],
    inject: false
  }),
  new HtmlWebpackPlugin({
    template: './src/renderer/osr.html',
    filename: path.join(__dirname, './dist/renderer/osr.html'),
    inject: false
  }),
  new HtmlWebpackPlugin({
    template: './src/renderer/exclusive.html',
    filename: path.join(__dirname, './dist/exclusive/exclusive.html'),
    chunks: ['exclusive'],
    inject: false
  })
];

rendererConfig.plugins.push(...htmlPlugins);

module.exports = rendererConfig;