const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function harness({status = 200, reduced = false} = {}) {
  const elements = new Map();
  let finish;
  let animationOptions;
  let frames;
  let delay;
  let openRequests = 0;
  const el = key => {
    if (!elements.has(key)) elements.set(key, {
      innerHTML: '', textContent: '', style: {}, clientWidth: 750,
      classList: {add() {}, remove() {}}, addEventListener() {}, setAttribute() {},
      scrollIntoView() {}, append() {}, close() {this.open = false;},
      showModal() {this.open = true;},
      children: Array.from({length:42}, (_, i) => ({
        offsetLeft:i * 156, offsetWidth:144, classList:{add() {}}
      })),
      animate(keyframes, options) {
        frames = keyframes;
        animationOptions = options;
        return {finished: new Promise(resolve => {finish = resolve;}), cancel() {}};
      }
    });
    return elements.get(key);
  };
  const drop = {id:'iron_helmet', material:'IRON_HELMET', image:'iron_helmet', rarity:'uncommon', chance:35};
  const other = {id:'netherite_helmet', material:'NETHERITE_HELMET', image:'netherite_helmet', rarity:'legendary', chance:5};
  const listeners = {};
  const context = {
    document: {querySelector:el, querySelectorAll:()=>[], createElement:()=>({}), title:''},
    location: {hash:'#case/armour'},
    history: {replaceState(a, b, hash) {context.location.hash = hash;}},
    window: {addEventListener(name, listener) {listeners[name] = listener;}, scrollTo() {}},
    matchMedia:()=>({matches:reduced}),
    AbortSignal, URLSearchParams, Math,
    setTimeout(fn, ms) {delay = ms; finish = fn;},
    fetch: async (url, options) => {
      if (url === '/api/cases') return {ok:true, json:async()=>({armour:[drop, other], weapon:[drop], tool:[drop]})};
      openRequests++;
      assert.equal(options.body.get('caseId'), 'armour');
      assert.equal(options.body.has('material'), false);
      return {ok:status===200, status, json:async()=>status===200 ? drop : {message:'Player is offline'}};
    }
  };
  vm.createContext(context);
  vm.runInContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/app.js'), 'utf8'), context);
  return {el, context, listeners, run:source=>vm.runInContext(source, context),
    finish:()=>finish(), options:()=>animationOptions, frames:()=>frames,
    delay:()=>delay, requests:()=>openRequests};
}

const flush = async () => {for(let i=0;i<10;i++) await Promise.resolve();};

test('reel moves left for 3000ms, lands on server reward and reveals only after finishing', async () => {
  const h = harness(); await flush();
  const pending = h.run('openCase(caseData[0])'); await flush();
  assert.equal(h.options().duration, 3000);
  assert.equal(h.options().easing, 'cubic-bezier(0.12, 0.8, 0.18, 1)');
  assert.equal(h.frames()[0].transform, 'translateX(0px)');
  assert.equal(h.frames()[1].transform, 'translateX(-5313px)');
  assert.equal(h.el('#result-dialog').open, undefined);
  assert.equal(h.el('#open-case').disabled, true);
  const cards = h.el('.reel-track').innerHTML.split('<div class="reel-item ').slice(1);
  assert.equal(cards.length, 42);
  assert.match(cards[36], /Iron Helmet/);
  await h.run('openCase(caseData[0])');
  assert.equal(h.requests(), 1, 'double clicks cannot deliver another reward');
  h.context.location.hash = '#cases'; h.listeners.hashchange();
  assert.equal(h.context.location.hash, '#case/armour');
  h.finish(); await pending;
  assert.equal(h.el('#result-dialog').open, true);
  assert.match(h.el('#result-content').innerHTML, /Iron Helmet/);
  assert.equal(h.el('#open-case').disabled, false);
  assert.equal(h.el('#open-case').textContent, 'Open!');
});

test('failed delivery never animates or claims an item was received', async () => {
  const h = harness({status:409}); await flush();
  await h.run('openCase(caseData[0])');
  assert.equal(h.options(), undefined);
  assert.equal(h.el('#result-dialog').open, undefined);
  assert.equal(h.el('#opening-status').textContent, 'Player is offline');
  assert.equal(h.el('#open-case').disabled, false);
});

test('reduced motion still waits three seconds before showing the result', async () => {
  const h = harness({reduced:true}); await flush();
  const pending = h.run('openCase(caseData[0])'); await flush();
  assert.equal(h.options(), undefined);
  assert.equal(h.delay(), 3000);
  assert.equal(h.el('#result-dialog').open, undefined);
  h.finish(); await pending;
  assert.equal(h.el('#result-dialog').open, true);
});
