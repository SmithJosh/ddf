const React = require('react');

const Group = require('./group');

const CustomElements = require('js/CustomElements');
const Component = CustomElements.registerReact('text-area');
const lineHeight = 22;
const maxRows = 10;

/* An auto-resizing textarea */
class TextArea extends React.Component {
    constructor(props) {
      super(props);
      this.state = {rows: 1};
      this.ref = React.createRef();
    }

    handleChange(event) {
        const newValue = event.target.value.replace(new RegExp("\r?\n|\r"), '');
        event.target.value = newValue;

        const oldRows = event.target.rows;
        event.target.rows = 1;
        var newRows = ~~(event.target.scrollHeight/lineHeight);

        if (newRows === oldRows) {
            event.target.rows = oldRows;
        } else {
            if (newRows > oldRows) {
                newRows = Math.min(newRows, maxRows);
            }
            event.target.rows = newRows;
            this.setState({
                rows: newRows
            });
        }

        this.props.onChange(newValue);
    }

    handleKeyPress(e) {
        if (e.which === 13) {
            e.preventDefault();
            e.stopPropagation();
        }
    }

    render() {
        return (
            <Component>
                <textarea
                    ref={this.ref}
                    value={this.props.value}
                    rows={this.state.rows}
                    onChange={this.handleChange.bind(this)}
                    onKeyPress={this.handleKeyPress.bind(this)}
                    style={{lineHeight: `${lineHeight}px`}}
                />
            </Component>
        );
    }
}

module.exports = TextArea;
