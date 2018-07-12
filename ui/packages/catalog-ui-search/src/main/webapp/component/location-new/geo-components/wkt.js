const React = require('react');
const { Units } = require('../common');
const { TextArea } = require('../inputs');

const WKT = (props) => {
    const { wkt, setState } = props;

    return (
        <div className="input-location">
            <TextArea
                value={wkt}
                onChange={setState((draft, value) => draft.wkt = value)}
            />
        </div>
    );
};

module.exports = WKT;
