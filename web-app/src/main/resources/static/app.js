'use strict';
const rarities = ['common', 'uncommon', 'epic', 'legendary'];
const labels = {common:'Common',uncommon:'Uncommon',epic:'Epic',legendary:'Legendary'};
// Item lists and exact probabilities come from the server-side enums.
const caseData = [
  {id:'armour',name:'Armour case',tag:'BUILT TO PROTECT',accent:'#7fe1ca',description:'Stand your ground. From iron essentials to the strength of netherite.',items:[]},
  {id:'weapon',name:'Weapon case',tag:'MADE FOR BATTLE',accent:'#be89ff',description:'Make every hit count. Discover your next adventure’s greatest ally.',items:[]},
  {id:'tool',name:'Tool case',tag:'DIG SOMETHING GREAT',accent:'#efc479',description:'Go a little deeper. Unearth the tools for your next big discovery.',items:[]}
];
const main = document.querySelector('#main');
const dialog = document.querySelector('#result-dialog');
let opening = false;
let catalogReady = false;
let catalogError = '';
const spriteNames = new Map();
const percent = value => Number(value).toFixed(2).replace(/\.00$/, '');
async function loadCatalog() {
  catalogError = '';
  try {
    const response = await fetch('/api/cases', {signal: AbortSignal.timeout(10000)});
    if (!response.ok) throw new Error('Unable to load the cases.');
    const catalogue = await response.json();
    for (const c of caseData) {
      if (!Array.isArray(catalogue[c.id]) || !catalogue[c.id].length) throw new Error('The case catalogue is incomplete.');
      c.items = catalogue[c.id].map(drop => {
        spriteNames.set(drop.id, drop.image);
        return [drop.id, drop.chance, rarities.indexOf(drop.rarity)];
      });
    }
    catalogReady = true;
  } catch (error) {
    catalogError = 'Cannot load cases. Make sure the web app is running, then try again.';
  }
  route();
}
const nameOf = id => id.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join(' ');
const icon = (id, extra='') => `<img src="images/item/${spriteNames.get(id) || id}.png" alt="${nameOf(id)}" ${extra}>`;
const legend = () => `<div class="legend">${rarities.map(r=>`<span class="${r}"><i></i>${labels[r]}</span>`).join('')}</div>`;
function catalogue(){
  main.innerHTML = `<section class="intro"><div><p class="eyebrow">THE NEXT ADVENTURE STARTS HERE</p><h1>Small chest.<br><em>Big possibilities.</em></h1><p>Pick your case. Explore the loot. Find your next upgrade.</p></div><div class="edition"><i></i><div>The starter collection<small>3 cases · 4 rarities · endless adventure</small></div></div></section><div class="section-heading"><h2>Choose your next drop</h2><span>COLLECTION 01 / THE ESSENTIALS</span></div><section class="case-grid" aria-label="Available cases">${caseData.map((c,i)=>`<a href="#case/${c.id}" class="case-card" style="--accent:${c.accent}"><div class="case-art"><span class="case-number">CASE / 0${i+1}</span><span class="case-label">${c.tag}</span><img src="images/cases/${c.id}.png" alt="Open Minecraft-style ${c.name.toLowerCase()}" width="600" height="600"></div><div class="case-info"><h3>${c.name}</h3><p>${c.description}</p><div class="case-meta"><div>${c.items.length} possible drops<div class="dots">${rarities.map(r=>`<i class="${r}"></i>`).join('')}</div></div><strong>Explore case ↗</strong></div></div></a>`).join('')}</section><section class="rarity-guide" id="rarity-guide"><div><h2>Every colour tells a story.</h2><p>From everyday essentials to extraordinary finds.</p></div>${legend()}</section>`;
}
function detail(c){
  main.innerHTML = `<a class="back" href="#cases">← Back to cases</a><section class="detail-hero" style="--accent:${c.accent}"><img class="detail-image" id="opening-image" src="images/cases/${c.id}.png" alt="${c.name}"><div class="detail-copy"><p class="eyebrow">${c.tag}</p><h1>${c.name}</h1><p>${c.description}</p><div><span class="pill">${c.items.length} possible drops</span><span class="pill">1 item per opening</span></div><button class="button primary" id="open-case">Open!</button><p class="fine">Join the Minecraft server and leave one inventory slot free.</p><p id="opening-status" class="form-status" role="status" aria-live="polite"></p></div></section><section class="reel" aria-label="Case opening reel"><div class="reel-window"><div class="reel-track" aria-hidden="true">${c.items.slice(0,8).map(reelCard).join('')}</div><div class="reel-marker" aria-hidden="true"></div></div><p class="reel-caption">What will you uncover?</p></section><div class="section-heading"><h2>Inside this case</h2><span>Every item. Every chance. No surprises.</span></div>${legend()}<section class="item-grid" aria-label="Case contents and probabilities">${c.items.map(([id,chance,r])=>`<article class="item ${rarities[r]}"><span class="chance">${percent(chance)}%</span>${icon(id,'loading="lazy"')}<h3>${nameOf(id)}</h3><span class="rarity">${labels[rarities[r]]}</span></article>`).join('')}</section><p class="drop-note">Drop chances: Common 45% · Uncommon 35% · Epic 15% · Legendary 5%. Each opening is independent.</p>`;
  document.querySelector('#open-case').addEventListener('click',()=>openCase(c));
}
function reelCard([id, chance, rarity]) {
  return `<div class="reel-item ${rarities[rarity]}">${icon(id)}<strong>${nameOf(id)}</strong><span>${labels[rarities[rarity]]}</span></div>`;
}

async function openCase(c) {
  if (opening) return;
  opening = true;
  const button = document.querySelector('#open-case');
  const status = document.querySelector('#opening-status');
  button.disabled = true;
  button.textContent = 'Opening…';
  status.className = 'form-status';
  status.textContent = 'Preparing your drop…';
  try {
    // Only the server chooses the winner. Decorative reel cards do not select loot.
    const response = await fetch('/open-case', {
      method: 'POST',
      body: new URLSearchParams({caseId: c.id}),
      credentials: 'same-origin',
      signal: AbortSignal.timeout(12000)
    });
    const result = await response.json();
    if (!response.ok) {
      const error = new Error(result.message || 'Unable to open this case.');
      error.loginRequired = response.status === 401;
      throw error;
    }
    const winner = c.items.find(item => item[0] === result.id);
    if (!winner) throw new Error('Item delivered, but its image is unavailable. Check your inventory.');
    status.textContent = '';
    await animateReel(c, winner);
    // The result dialog is deliberately shown only after the 3-second reel finishes.
    document.querySelector('#result-content').innerHTML =
      `${icon(winner[0])}<p class="result-rarity ${rarities[winner[2]]}">${labels[rarities[winner[2]]]} · ${percent(winner[1])}% chance</p><h2 id="result-title">${nameOf(winner[0])}</h2>`;
    dialog.showModal();
    status.textContent = 'Item received! Check your Minecraft inventory.';
  } catch (error) {
    status.className = 'form-status error';
    status.textContent = ['TimeoutError','TypeError'].includes(error.name)
      ? 'Delivery could not be confirmed. Check your inventory before trying again.'
      : error.message;
    if (error.loginRequired) {
      const link = document.createElement('a');
      link.href = '#login'; link.textContent = ' Log in →'; status.append(link);
    }
  } finally {
    opening = false;
    button.disabled = false;
    button.textContent = 'Open!';
  }
}

async function animateReel(c, winner) {
  const viewport = document.querySelector('.reel-window');
  const track = document.querySelector('.reel-track');
  const caption = document.querySelector('.reel-caption');
  viewport.scrollIntoView({block: 'center', behavior: 'instant'});
  const winningIndex = 36;
  const cards = Array.from({length: 42}, () => c.items[Math.floor(Math.random() * c.items.length)]);
  cards[winningIndex] = winner;
  track.innerHTML = cards.map(reelCard).join('');
  track.style.transform = 'translateX(0px)';
  caption.textContent = 'Opening…';
  viewport.setAttribute('aria-busy', 'true');
  // Measurements include the actual gap and card width on desktop and mobile.
  const selected = track.children[winningIndex];
  const offset = () => viewport.clientWidth / 2 - selected.offsetLeft - selected.offsetWidth / 2;
  const destination = offset();
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) {
    track.style.transform = `translateX(${destination}px)`;
    await new Promise(resolve => setTimeout(resolve, 3000));
  } else {
    const animation = track.animate(
      [{transform: 'translateX(0px)'}, {transform: `translateX(${destination}px)`}],
      {duration: 3000, easing: 'cubic-bezier(0.12, 0.8, 0.18, 1)', fill: 'forwards'}
    );
    await animation.finished;
    // Re-center after a resize during the spin, without starting another spin.
    track.style.transform = `translateX(${offset()}px)`;
    animation.cancel();
  }
  selected.classList.add('winner');
  viewport.setAttribute('aria-busy', 'false');
  caption.textContent = 'Item received!';
}
function auth(signup){
  main.innerHTML=`<section class="auth-layout"><div class="auth-art"><img src="images/cases/armour.png" alt="Glowing diamond armour in an open chest"><div class="auth-caption"><p class="eyebrow">YOUR NEXT CHAPTER</p><h2>Great adventures<br>start with better gear.</h2><p>Discover what’s waiting inside the vault.</p></div></div><div class="auth-form"><a class="back" href="#cases">← Explore the cases</a><p class="eyebrow">WELCOME TO BLOCKVAULT</p><h1>${signup?'Your adventure awaits.':'Welcome back.'}</h1><p class="muted">${signup?'Create your account and make yourself at home.':'Log in to your account to continue.'}</p><div class="auth-tabs"><a href="#login" class="${signup?'':'active'}" ${signup?'':'aria-current="page"'}>Log in</a><a href="#signup" class="${signup?'active':''}" ${signup?'aria-current="page"':''}>Sign up</a></div><form id="account-form" action="${signup?'/add':'/login'}" method="post"><label for="login">Minecraft username</label><input id="login" name="login" autocomplete="username" placeholder="Your in-game name" required maxlength="64"><label for="password">Password</label><div class="password-wrap"><input id="password" name="password" type="password" autocomplete="${signup?'new-password':'current-password'}" placeholder="Enter your password" required><button type="button" id="toggle-password" aria-label="Show password">Show</button></div><button class="button primary" type="submit">${signup?'Create account':'Log in'} <span>→</span></button><p id="form-status" class="form-status" role="status" aria-live="polite"></p></form><p class="fine">Use the same username as your Minecraft player.</p></div></section>`;
  document.querySelector('#toggle-password').addEventListener('click',event=>{
    const input=document.querySelector('#password');const show=input.type==='password';input.type=show?'text':'password';event.target.textContent=show?'Hide':'Show';event.target.setAttribute('aria-label',show?'Hide password':'Show password');
  });
  document.querySelector('#account-form').addEventListener('submit',async event=>{
    event.preventDefault();const form=event.currentTarget;const status=document.querySelector('#form-status');const button=form.querySelector('[type="submit"]');button.disabled=true;status.className='form-status';status.textContent=signup?'Creating your account…':'Logging in…';
    try{
      const response=await fetch(signup?'/add':'/login',{method:'POST',body:new URLSearchParams(new FormData(form)),credentials:'same-origin',signal:AbortSignal.timeout(12000)});
      if(!response.ok)throw new Error(response.status===401?'Invalid username or password.':response.status===409?'This username is already taken.':'Unable to complete your request. Please try again.');
      if(!form.isConnected)return;
      if(signup){status.textContent='Account created. You can now log in.';form.reset();const link=document.createElement('a');link.href='#login';link.textContent=' Go to login →';status.append(link);}
      else{document.querySelector('#account-link').textContent='Account ✓';location.hash='#cases';}
    }catch(error){status.className='form-status error';status.textContent=['TimeoutError','TypeError'].includes(error.name)?'Cannot reach the server. Please check that the web app is running.':error.message;}finally{button.disabled=false;}
  });
}
function route(){
  if (opening) return;
  if(dialog.open)dialog.close();
  const hash=location.hash.slice(1)||'cases';
  if(hash==='login'||hash==='signup'){auth(hash==='signup');document.title=`${hash==='signup'?'Sign up':'Log in'} — Blockvault`;}
  else if (!catalogReady) {
    main.innerHTML = `<section class="intro"><div><p class="eyebrow">BLOCKVAULT</p><h1>${catalogError ? 'Cases unavailable' : 'Opening the vault…'}</h1><p role="status">${catalogError || 'Loading the latest items and drop chances.'}</p>${catalogError ? '<button class="button primary" id="retry-catalog">Try again</button>' : ''}</div></section>`;
    document.querySelector('#retry-catalog')?.addEventListener('click', loadCatalog);
  }
  else if(hash.startsWith('case/')&&caseData.some(c=>c.id===hash.split('/')[1])){const c=caseData.find(c=>c.id===hash.split('/')[1]);detail(c);document.title=`${c.name} — Blockvault`;}
  else{catalogue();document.title='Blockvault — Find your next upgrade';}
  if(hash==='rarities')document.querySelector('#rarity-guide')?.scrollIntoView();else window.scrollTo(0,0);
}
document.querySelectorAll('.close,.close-result').forEach(button=>button.addEventListener('click',()=>dialog.close()));
dialog.addEventListener('click',event=>{if(event.target===dialog){const rect=dialog.getBoundingClientRect();if(event.clientX<rect.left||event.clientX>rect.right||event.clientY<rect.top||event.clientY>rect.bottom)dialog.close();}});
window.addEventListener('hashchange', () => {
  if (opening) {
    // Keep the reel mounted while the request/animation is in progress.
    history.replaceState(null, '', currentRoute);
    return;
  }
  currentRoute = location.hash || '#cases';
  route();
});
let currentRoute = location.hash || '#cases';
route();
loadCatalog();
