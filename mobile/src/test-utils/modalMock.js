const React = require('react');
const { View } = require('react-native');

function MockModal(props) {
  if (!props.visible) return null;
  return React.createElement(View, { testID: 'mock-modal' }, props.children);
}

module.exports = MockModal;
module.exports.default = MockModal;
