import { Selector } from 'testcafe';

fixture `End-to-end test`
    .page `http://localhost:5000`;

const subscription = Selector('input#subscription');
const options = {
    timeout: 30000
};

test('End-to-end test', async t => {
    await t
        .expect(subscription.value).notEql('', 'subscription input is empty', options)
        .click('#send')
        .expect(Selector('li').exists).ok('', options);
});
