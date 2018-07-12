const React = require('react');

const AutoComplete = require('./auto-complete');
const Button = require('./button');
const Dropdown = require('./dropdown');
const Group = require('./group');
const TextArea = require('./text-area');
const TextField = require('./text-field');
const { Menu, MenuItem } = require('./menu');
const { Radio, RadioItem } = require('./radio');

const Label = ({ children }) => <span className="input-group-addon">{children}&nbsp;</span>;

module.exports = {
    Button,
    Radio,
    RadioItem,
    Menu,
    MenuItem,
    Dropdown,
    Label,
    Group,
    TextArea,
    TextField,
    AutoComplete
};
