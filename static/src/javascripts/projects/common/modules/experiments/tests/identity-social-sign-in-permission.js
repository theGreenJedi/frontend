/**
 * Defines test to record usage of new Social sign in messaging variant for Identity.
 *
 * Code for recording test events is in the new identity-frontend repo, and will
 * run automatically when users are routed to the new service.
 *
 * @see https://github.com/guardian/identity-frontend
 */
define([], function () {

    return function () {

        this.id = 'IdentitySocialSigninPermission';
        this.start = '2015-03-15';
        this.expiry = '2016-04-05';
        this.author = 'Mark Butler';
        this.description = 'New social sign in messaging variant for Identity';
        this.audience = 0.5;
        this.audienceOffset = 0.0;
        this.successMeasure = 'Users are more likely to sign in using social buttons';
        this.audienceCriteria = 'everyone';
        this.dataLinkNames = '';
        this.idealOutcome = 'More people sign in using social buttons';

        this.canRun = function () {
            // Test data will be recorded automatically when run on new identity-frontend service.
            return false;
        };

        this.variants = [
            {
                id: 'control',
                test: function () {}
            },
            {
                id: 'A',
                test: function () {}
            },
            {
                id: 'B',
                test: function () {}
            }
        ];

    };

});
