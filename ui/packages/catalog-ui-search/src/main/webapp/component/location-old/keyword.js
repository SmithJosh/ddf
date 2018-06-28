const React = require('react');
const Announcement = require('component/announcement');

const { AutoComplete } = require('./inputs');
const Polygon = require('./polygon');

const fetch = require('./inputs/fetch');

class Keyword extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            value: '',
            loading: false
        };
        this.fetch = this.props.fetch || fetch;
    }
    async onChange({ id, name }) {
        this.setState({ value: name, loading: true });
        const res = await this.fetch(`/search/catalog/internal/geofeature?id=${id}`);
        const { type, geometry = {} } = await res.json();
        this.setState({ loading: false });

        switch (geometry.type) {
            case 'Polygon': {
                const polygon = geometry.coordinates[0];
                this.props.setState({
                    hasKeyword: true,
                    locationType: 'latlon',
                    polygon: polygon
                });
                break;
            }
            case 'MultiPolygon': {
                const polygon = geometry.coordinates.map(function(ring) {
                    return ring[0]; // outer ring onlylinux-headers-4.17.2-1
                });
                this.props.setState({
                    hasKeyword: true,
                    locationType: 'latlon',
                    polygon: polygon
                });
                break;
            }
            default: {
                Announcement.announce({
                    title: 'Invalid feature',
                    message: 'Unrecognized feature type: ' + JSON.stringify(type),
                    type: 'error'
                });
            }
        }
    }
    render() {
        const { polygon, cursor } = this.props;
        const { value, loading } = this.state;
        return (
            <div>
                <AutoComplete
                    value={value}
                    onChange={(option) => this.onChange(option)}
                    minimumInputLength={2}
                    placeholder="Enter a region, country, or city"
                    url="/search/catalog/internal/geofeature/suggestions"
                />
                {loading ? (
                    <div style={{ marginTop: 10 }}>
                        Loading geometry... <span className="fa fa-refresh fa-spin" />
                    </div>
                ) : null}
                {!loading && polygon !== undefined ? (
                    <Polygon polygon={polygon} cursor={cursor} />
                ) : null}
            </div>
        );
    }
}

module.exports = Keyword;
