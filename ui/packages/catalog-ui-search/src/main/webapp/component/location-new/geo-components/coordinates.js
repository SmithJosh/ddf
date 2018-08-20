const React = require('react');

const TextField = require('react-component/text-field');
const MaskedTextField = require('../inputs/masked-text-field');
const { latitudeDMSMask, longitudeDMSMask } = require('./masks');

class Coordinate extends React.Component {
    render() {
        const { placeholder, value, onChange, children, ...props } = this.props;
        return (
            <div className="coordinate">
                <TextField
                    placeholder={placeholder}
                    value={value}
                    onChange={onChange}
                    {...props}
                />
                {children}
            </div>
        );
    }
}

class MaskedCoordinate extends React.Component {
    render() {
        const { placeholder, mask, value, onChange, children, ...props } = this.props;
        return (
            <div className="coordinate">
                <MaskedTextField
                    placeholder={placeholder}
                    mask={mask}
                    value={value}
                    onChange={onChange}
                    {...props}
                />
                {children}
            </div>
        );
    }
}

const DmsLatitude = (props) => {
    return (
        <MaskedCoordinate
            placeholder="dd°mm'ss.s&quot;"
            mask={latitudeDMSMask}
            {...props}
        />
    );
};

const DmsLongitude = (props) => {
    return (
        <MaskedCoordinate
            placeholder="ddd°mm'ss.s&quot;"
            mask={longitudeDMSMask}
            {...props}
        />
    );
};

const DdLatitude = (props) => {
    return (
        <Coordinate
            placeholder="latitude"
            type="number"
            step="any"
            min={-90}
            max={90}
            addon="°"
            {...props}
        />
    );
};

const DdLongitude = (props) => {
    return (
        <Coordinate
            placeholder="longitude"
            type="number"
            step="any"
            min={-180}
            max={180}
            addon="°"
            {...props}
        />
    );
};

const UsngCoordinate = (props) => {
    return (
        <div className="coordinate">
            <TextField
                label="Grid"
                {...props}
            />
        </div>
    );
};

module.exports = {
    DmsLatitude,
    DmsLongitude,
    DdLatitude,
    DdLongitude,
    UsngCoordinate
};