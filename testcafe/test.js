import { Selector } from 'testcafe';

fixture `End-to-end test`
    .page `http://localhost:5000`;

test('End-to-end test', async t => {
    await t
        .expect(Selector('input#subscription').value).notEql('', 'subscription input is empty', { timeout: 100000 })
        .click('#send')
        .expect(Selector('li').exists).ok('', { timeout: 100000 });
});
