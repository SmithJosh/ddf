/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'marionette',
    'underscore',
    'jquery',
    '../dropdown.view',
    './dropdown.result-filter.hbs',
    'component/result-filter/result-filter.view',
    'component/singletons/user-instance'
], function (Marionette, _, $, DropdownView, template, ComponentView, user) {

    return DropdownView.extend({
        attributes: {
            'data-help': 'Used to setup a local filter of a result set.  It does not re-execute the query.'
        },
        template: template,
        className: 'is-resultFilter',
        componentToShow: ComponentView,
        initialize: function(){
            DropdownView.prototype.initialize.call(this);
            this.listenTo(user.get('user').get('preferences'), 'change:resultFilter', this.handleFilter);
            this.handleFilter();
        },
        handleFilter: function(){
            var resultFilter = user.get('user').get('preferences').get('resultFilter');
            this.$el.toggleClass('has-filter', Boolean(resultFilter));
        }
    });
});
