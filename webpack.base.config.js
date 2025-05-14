const path = require('path');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

module.exports = {
  mode: 'development',
  devtool: 'source-map',
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/
      }
    ]
  },
  resolve: {
    extensions: ['.ts', '.tsx', '.json', '.js']
  },
  output: {
    path: path.join(__dirname, '/dist'),
    filename: '[name]/[name].js'
  },
  plugins: [new CleanWebpackPlugin()],
  externals: {
    bufferutil: 'bufferutil',
    'utf-8-validate': 'utf-8-validate'
  }
};