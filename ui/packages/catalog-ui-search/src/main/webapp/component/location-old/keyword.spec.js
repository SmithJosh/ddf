const React = require('react');

const Enzyme = require('enzyme');
const Adapter = require('enzyme-adapter-react-16');

const { expect } = require('chai');

Enzyme.configure({ adapter: new Adapter() });

const Keyword = require('./keyword');
const AutoComplete = require('./inputs/auto-complete');

const { shallow } = Enzyme;

describe('<Keyword />', () => {
    it('should fetch features on select', (done) => {
        const coordinates = [1, 2, 3];
        const fetch = async (url) => {
            expect(url).to.equal('/search/catalog/internal/geofeature?id=0');
            return {
                async json() {
                    return {
                        geometry: {
                            type: 'Polygon',
                            coordinates: [coordinates]
                        }
                    };
                }
            };
        };
        const setState = ({ polygon }) => {
            expect(wrapper.state('loading')).to.equal(false);
            expect(polygon).to.equal(coordinates);
            done();
        };
        const wrapper = shallow(<Keyword fetch={fetch} setState={setState} />);
        wrapper.find(AutoComplete).prop('onChange')({
            id: '0',
            name: 'test'
        });
        expect(wrapper.state('loading')).to.equal(true);
    });
});
