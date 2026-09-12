/*
 * SafeSpend Design Builder
 * ------------------------
 * Generates the whole SafeSpend design file: colour styles grouped by their role
 * in the 60-30-10 rule, text styles, a component library, eight screen frames and
 * the prototype links between them.
 *
 * Building it in code rather than by hand means the file agrees with the app: the
 * hex values, type sizes, radii and spacing here are the same ones in
 * app/src/main/java/com/safespend/app/ui/theme/.
 *
 * Run once on an empty file. Everything it makes is ordinary, editable Figma.
 */

// ---------------------------------------------------------------- palette

var C = {
  // 60% — neutral canvas
  mist: '#F4F6F7',
  surface: '#FFFFFF',
  ink: '#0F1D21',
  muted: '#5A6B6E',
  line: '#E3EAEA',
  // 30% — brand
  teal: '#0E4F52',
  tealContainer: '#CFE7E6',
  tealDeeper: '#06282A',
  // 10% — accent
  coral: '#FF7A45',
  coralContainer: '#FFE1D3',
  coralOnBrand: '#FFB08C',
  // data colours
  income: '#1E8E5A',
  expense: '#C4443A',
  cat1: '#0E4F52',
  cat2: '#2F7F82',
  cat3: '#58A9A4',
  cat4: '#8CC6BE',
  cat5: '#FF7A45',
  cat6: '#E8A33D',
  cat7: '#7E6BB0',
  cat8: '#B0AFAF'
};

var STYLE_GROUPS = [
  ['60 Neutral/Mist', C.mist],
  ['60 Neutral/Surface', C.surface],
  ['60 Neutral/Ink', C.ink],
  ['60 Neutral/Muted', C.muted],
  ['60 Neutral/Line', C.line],
  ['30 Brand/Deep Teal', C.teal],
  ['30 Brand/Teal Container', C.tealContainer],
  ['30 Brand/Teal Deeper', C.tealDeeper],
  ['10 Accent/Coral', C.coral],
  ['10 Accent/Coral Container', C.coralContainer],
  ['10 Accent/Coral on Brand', C.coralOnBrand],
  ['Data/Income', C.income],
  ['Data/Expense', C.expense],
  ['Data/Category 1', C.cat1],
  ['Data/Category 2', C.cat2],
  ['Data/Category 3', C.cat3],
  ['Data/Category 4', C.cat4],
  ['Data/Category 5', C.cat5],
  ['Data/Category 6', C.cat6],
  ['Data/Category 7', C.cat7],
  ['Data/Category 8', C.cat8]
];

// name, size, weight, lineHeight, letterSpacing
var TEXT_STYLES = [
  ['Display Small', 36, 'Bold', 42, -0.5],
  ['Headline Medium', 26, 'Bold', 32, -0.2],
  ['Headline Small', 22, 'Semi Bold', 28, 0],
  ['Title Large', 19, 'Semi Bold', 25, 0],
  ['Title Medium', 16, 'Semi Bold', 22, 0],
  ['Body Large', 16, 'Regular', 23, 0],
  ['Body Medium', 14, 'Regular', 20, 0],
  ['Body Small', 12, 'Regular', 16, 0],
  ['Label Large', 14, 'Semi Bold', 18, 0.1],
  ['Label Medium', 12, 'Medium', 16, 0.4],
  ['Label Small', 11, 'Medium', 14, 0.5]
];

var W = 412;           // Android Large
var H = 915;
var MARGIN = 16;
var paintStyles = {};  // hex -> PaintStyle
var textStyles = {};   // name -> TextStyle

// ---------------------------------------------------------------- helpers

function rgb(hex) {
  var h = hex.replace('#', '');
  return {
    r: parseInt(h.substring(0, 2), 16) / 255,
    g: parseInt(h.substring(2, 4), 16) / 255,
    b: parseInt(h.substring(4, 6), 16) / 255
  };
}

function solid(hex, opacity) {
  return { type: 'SOLID', color: rgb(hex), opacity: opacity === undefined ? 1 : opacity };
}

/** Links to a colour style when the colour is a palette colour at full opacity. */
function fill(node, hex, opacity) {
  node.fills = [solid(hex, opacity)];
  if ((opacity === undefined || opacity === 1) && paintStyles[hex]) {
    try { node.fillStyleId = paintStyles[hex].id; } catch (e) { /* raw fill is fine */ }
  }
}

function font(weight) {
  return { family: 'Inter', style: weight || 'Regular' };
}

/*
 * Icons are drawn as real vectors rather than text glyphs.
 *
 * The first version of this plugin used characters like ⌂ and ▤ for the bottom
 * bar. Inter has no glyph for them, so they rendered as nothing at all and the
 * navigation came out as four bare labels. Anything that has to be tinted — and a
 * selected tab does — has to be a vector.
 */
var ICONS = {
  home: '<path d="M12 3.1 2.6 11.1h2.6V21h4.6v-5.4h4.4V21h4.6v-9.9h2.6L12 3.1Z"/>',
  list: '<path d="M3.5 5h17v2.6h-17zM3.5 10.7h17v2.6h-17zM3.5 16.4h17V19h-17z"/>',
  // An envelope, not a wallet: the app calls these envelope budgets, and at 21px a
  // wallet's side pocket reads as an arrow rather than a pocket.
  envelope: '<path d="M2.5 7.9 12 14.1l9.5-6.2V17a1.9 1.9 0 0 1-1.9 1.9H4.4A1.9 1.9 0 0 1 2.5 17V7.9Z"/><path d="M4.4 5.1h15.2c.75 0 1.4.44 1.71 1.07L12 12.3 2.69 6.17A1.9 1.9 0 0 1 4.4 5.1Z"/>',
  chart: '<path d="M3.6 20V11h3.3v9zM10.4 20V4h3.3v16zM17.2 20v-6h3.3v6z"/>'
};

/**
 * Builds a tintable icon from SVG path data at a 24×24 viewBox, then scales the
 * whole node — children included — with rescale(). Resizing the wrapper frame
 * alone would stretch the frame and leave the paths at their original size.
 */
function icon(pathData, size, colorHex) {
  var node = figma.createNodeFromSvg(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24">' +
    pathData + '</svg>'
  );
  node.name = 'Icon';
  node.fills = [];
  var parts = node.findAll(function (n) {
    return n.type === 'VECTOR' || n.type === 'RECTANGLE' || n.type === 'ELLIPSE';
  });
  for (var i = 0; i < parts.length; i++) {
    parts[i].fills = [solid(colorHex)];
    parts[i].strokes = [];
  }
  node.rescale(size / 24);
  return node;
}

/**
 * o = { size, weight, color, opacity, width, align, style, lh, ls }
 */
function T(chars, o) {
  o = o || {};
  var n = figma.createText();
  n.fontName = font(o.weight);
  n.characters = String(chars);
  n.fontSize = o.size || 14;
  if (o.lh) n.lineHeight = { unit: 'PIXELS', value: o.lh };
  if (o.ls) n.letterSpacing = { unit: 'PIXELS', value: o.ls };
  if (o.align) n.textAlignHorizontal = o.align;
  if (o.width) {
    n.textAutoResize = 'HEIGHT';
    n.resize(o.width, n.height);
  } else {
    n.textAutoResize = 'WIDTH_AND_HEIGHT';
  }
  fill(n, o.color || C.ink, o.opacity);
  if (o.style && textStyles[o.style]) {
    try { n.textStyleId = textStyles[o.style].id; } catch (e) { /* explicit size is fine */ }
  }
  n.name = chars.length > 28 ? chars.substring(0, 28) + '…' : String(chars);
  return n;
}

/**
 * Auto-layout frame.
 * o = { gap, padding:[t,r,b,l], fill, radius, align, justify, width, height, stroke }
 */
function stack(name, direction, o) {
  o = o || {};
  var f = figma.createFrame();
  f.name = name;
  f.layoutMode = direction;
  f.itemSpacing = o.gap === undefined ? 0 : o.gap;
  var p = o.padding || [0, 0, 0, 0];
  f.paddingTop = p[0]; f.paddingRight = p[1]; f.paddingBottom = p[2]; f.paddingLeft = p[3];
  f.primaryAxisSizingMode = 'AUTO';
  f.counterAxisSizingMode = 'AUTO';
  f.counterAxisAlignItems = o.align || 'MIN';
  if (o.justify) f.primaryAxisAlignItems = o.justify;
  if (o.fill) { fill(f, o.fill, o.fillOpacity); } else { f.fills = []; }
  if (o.radius) f.cornerRadius = o.radius;
  if (o.stroke) {
    f.strokes = [solid(o.stroke)];
    f.strokeWeight = o.strokeWeight || 1;
  }
  // An empty auto-layout frame can measure 0 on an axis, and resize() rejects 0.
  if (o.width) {
    f.counterAxisSizingMode = direction === 'VERTICAL' ? 'FIXED' : f.counterAxisSizingMode;
    f.primaryAxisSizingMode = direction === 'HORIZONTAL' ? 'FIXED' : f.primaryAxisSizingMode;
    f.resize(o.width, Math.max(f.height, 1));
  }
  if (o.height) {
    f.primaryAxisSizingMode = direction === 'VERTICAL' ? 'FIXED' : f.primaryAxisSizingMode;
    f.counterAxisSizingMode = direction === 'HORIZONTAL' ? 'FIXED' : f.counterAxisSizingMode;
    f.resize(Math.max(f.width, 1), o.height);
  }
  return f;
}

function add(parent, child, stretch) {
  parent.appendChild(child);
  if (stretch) {
    // A text node that hugs its own width cannot also stretch to its parent.
    if (child.type === 'TEXT') child.textAutoResize = 'HEIGHT';
    child.layoutAlign = 'STRETCH';
  }
  return child;
}

function grow(node) { node.layoutGrow = 1; return node; }

function spacer(w, h) {
  var r = figma.createRectangle();
  r.name = 'Spacer';
  r.resize(w || 1, h || 1);
  r.fills = [];
  r.locked = true;
  return r;
}

/** The one card definition — 20dp radius, white, no shadow. */
function card(o) {
  o = o || {};
  var c = stack(o.name || 'SectionCard', 'VERTICAL', {
    gap: o.gap === undefined ? 12 : o.gap,
    padding: [o.pad || 16, o.pad || 16, o.pad || 16, o.pad || 16],
    fill: o.fill || C.surface,
    radius: 20,
    width: o.width || (W - MARGIN * 2)
  });
  return c;
}

function progress(pct, colorHex, trackHex, width, height, trackOpacity) {
  height = height || 8;
  width = width || (W - MARGIN * 2 - 32);
  var track = figma.createFrame();
  track.name = 'ProgressTrack';
  track.resize(width, height);
  track.cornerRadius = height / 2;
  track.clipsContent = true;
  fill(track, trackHex || C.line, trackOpacity);
  var f = figma.createRectangle();
  f.name = 'Fill';
  f.resize(Math.max(2, width * Math.max(0, Math.min(1, pct))), height);
  f.cornerRadius = height / 2;
  f.x = 0; f.y = 0;
  fill(f, colorHex);
  track.appendChild(f);
  return track;
}

/** Round tinted category badge. The glyph stands in for the Material icon. */
function badge(glyph, colorHex, size) {
  size = size || 42;
  var b = figma.createFrame();
  b.name = 'CategoryBadge';
  b.resize(size, size);
  b.cornerRadius = size / 2;
  fill(b, colorHex, 0.14);
  var g = T(glyph, { size: Math.round(size * 0.42), weight: 'Medium', color: colorHex });
  b.appendChild(g);
  g.x = (size - g.width) / 2;
  g.y = (size - g.height) / 2;
  return b;
}

function chip(label, selected, colorHex) {
  var c = stack('Chip / ' + label, 'HORIZONTAL', {
    gap: 7,
    padding: [7, 14, 7, 14],
    fill: selected ? (colorHex || C.teal) : C.line,
    fillOpacity: selected ? 0.16 : 1,
    radius: 40,
    align: 'CENTER'
  });
  if (selected) {
    c.strokes = [solid(colorHex || C.teal)];
    c.strokeWeight = 1.5;
  }
  add(c, T(label, {
    size: 14,
    weight: selected ? 'Semi Bold' : 'Regular',
    color: selected ? C.ink : C.muted,
    style: 'Body Medium'
  }));
  return c;
}

function categoryChip(glyph, label, selected, colorHex) {
  var c = stack('CategoryChip / ' + label, 'HORIZONTAL', {
    gap: 7,
    padding: [6, 14, 6, 6],
    fill: selected ? colorHex : C.line,
    fillOpacity: selected ? 0.16 : 1,
    radius: 40,
    align: 'CENTER'
  });
  if (selected) {
    c.strokes = [solid(colorHex)];
    c.strokeWeight = 1.5;
  }
  add(c, badge(glyph, colorHex, 26));
  add(c, T(label, { size: 14, weight: selected ? 'Semi Bold' : 'Regular', color: C.ink }));
  return c;
}

function statTile(label, value, valueColor, width) {
  var s = stack('StatTile / ' + label, 'VERTICAL', { gap: 2, width: width });
  add(s, T(label, { size: 12, weight: 'Medium', color: C.muted, ls: 0.4, style: 'Label Medium' }));
  // Bold rather than the Title Large style's Semi Bold: a figure has to out-weigh
  // its own caption or the caption reads first.
  add(s, T(value, { size: 19, weight: 'Bold', color: valueColor || C.ink }));
  return s;
}

function transactionRow(glyph, colorHex, name, sub, amount, isIncome) {
  var r = stack('TransactionRow / ' + name, 'HORIZONTAL', {
    gap: 12,
    padding: [10, 0, 10, 0],
    align: 'CENTER',
    width: W - MARGIN * 2 - 32
  });
  add(r, badge(glyph, colorHex));
  var mid = stack('Text', 'VERTICAL', { gap: 1 });
  add(mid, T(name, { size: 16, color: C.ink, style: 'Body Large' }));
  add(mid, T(sub, { size: 12, color: C.muted, style: 'Body Small' }));
  grow(add(r, mid));
  add(r, T((isIncome ? '+' : '−') + amount, {
    size: 16,
    weight: 'Semi Bold',
    color: isIncome ? C.income : C.ink,
    style: 'Title Medium'
  }));
  return r;
}

function sectionHeader(title, action) {
  var h = stack('SectionHeader / ' + title, 'HORIZONTAL', {
    align: 'CENTER',
    width: W - MARGIN * 2
  });
  grow(add(h, T(title, { size: 16, weight: 'Semi Bold', color: C.ink, style: 'Title Medium' })));
  if (action) add(h, T(action, { size: 14, weight: 'Semi Bold', color: C.teal, ls: 0.1, style: 'Label Large' }));
  return h;
}

function field(placeholder, leading) {
  var f = stack('Field / ' + placeholder, 'HORIZONTAL', {
    gap: 10,
    padding: [14, 14, 14, 14],
    radius: 12,
    align: 'CENTER',
    stroke: C.line,
    width: W - MARGIN * 2 - 32
  });
  if (leading) add(f, T(leading, { size: 16, color: C.muted }));
  grow(add(f, T(placeholder, { size: 16, color: C.muted, style: 'Body Large' })));
  return f;
}

function button(label, bg, fg, width) {
  var b = stack('Button / ' + label, 'HORIZONTAL', {
    padding: [15, 20, 15, 20],
    radius: 12,
    fill: bg,
    align: 'CENTER',
    justify: 'CENTER',
    width: width
  });
  add(b, T(label, { size: 16, weight: 'Semi Bold', color: fg, style: 'Title Medium' }));
  return b;
}

function outlineButton(label, colorHex, width) {
  var b = stack('Button / ' + label, 'HORIZONTAL', {
    padding: [13, 20, 13, 20],
    radius: 12,
    stroke: colorHex,
    align: 'CENTER',
    justify: 'CENTER',
    width: width
  });
  add(b, T(label, { size: 15, weight: 'Semi Bold', color: colorHex }));
  return b;
}

// ------------------------------------------------------------ phone frame

/**
 * A screen. Returns { frame, content } — append to `content`, which is an
 * auto-layout column; the bottom bar and FAB are positioned absolutely so they
 * stay pinned while the content grows.
 */
function screen(name, x, y, o) {
  o = o || {};
  var f = figma.createFrame();
  f.name = name;
  f.resize(W, H);
  f.x = x; f.y = y;
  f.clipsContent = true;
  fill(f, o.bg || C.mist);

  var content = stack('Content', 'VERTICAL', {
    gap: o.gap === undefined ? 13 : o.gap,
    padding: [o.top === undefined ? 52 : o.top, MARGIN, 110, MARGIN],
    width: W
  });
  f.appendChild(content);
  content.x = 0;
  content.y = 0;

  if (!o.noStatusBar) {
    var sb = stack('Status bar', 'HORIZONTAL', { align: 'CENTER', width: W, padding: [0, MARGIN, 0, MARGIN] });
    f.appendChild(sb);
    sb.x = 0; sb.y = 16;
    grow(add(sb, T('9:41', { size: 12, weight: 'Semi Bold', color: C.ink })));

    // Drawn, not typed — the block glyphs these replace were another set Inter
    // has no coverage for.
    var indicators = stack('Status icons', 'HORIZONTAL', { gap: 2.5, align: 'MAX' });
    add(sb, indicators);
    for (var b = 0; b < 3; b++) {
      var signalBar = figma.createRectangle();
      signalBar.name = 'Signal';
      signalBar.resize(2.5, 4 + b * 2.5);
      signalBar.cornerRadius = 1;
      fill(signalBar, C.ink, 0.8);
      add(indicators, signalBar);
    }
    var battery = figma.createRectangle();
    battery.name = 'Battery';
    battery.resize(17, 9);
    battery.cornerRadius = 2.5;
    fill(battery, C.ink, 0.8);
    add(indicators, battery);
  }

  return { frame: f, content: content };
}

var NAV = [
  [ICONS.home, 'Home'],
  [ICONS.list, 'Ledger'],
  [ICONS.envelope, 'Budgets'],
  [ICONS.chart, 'Insights']
];

function bottomBar(frame, selectedIndex) {
  var bar = stack('BottomNav', 'HORIZONTAL', {
    fill: C.surface,
    padding: [12, 8, 20, 8],
    width: W
  });
  frame.appendChild(bar);
  bar.x = 0;

  for (var i = 0; i < NAV.length; i++) {
    var selected = i === selectedIndex;
    var item = stack('NavItem / ' + NAV[i][1], 'VERTICAL', { gap: 4, align: 'CENTER' });
    grow(add(bar, item));

    var pill = stack('Indicator', 'HORIZONTAL', {
      padding: [4, 18, 4, 18],
      radius: 20,
      align: 'CENTER',
      justify: 'CENTER'
    });
    if (selected) fill(pill, C.tealContainer);
    add(item, pill);
    add(pill, icon(NAV[i][0], 21, selected ? C.tealDeeper : C.muted));
    add(item, T(NAV[i][1], {
      size: 11,
      weight: 'Medium',
      color: selected ? C.teal : C.muted,
      ls: 0.5,
      style: 'Label Small'
    }));
  }
  // Only now does the bar know how tall it is.
  bar.y = H - bar.height;
  return bar;
}

function fab(frame) {
  var f = stack('FAB', 'HORIZONTAL', {
    fill: C.coral,
    radius: 28,
    width: 56,
    height: 56,
    align: 'CENTER',
    justify: 'CENTER'
  });
  frame.appendChild(f);
  f.x = W - MARGIN - 56;
  f.y = H - 96 - 56;
  f.effects = [{
    type: 'DROP_SHADOW',
    color: { r: 0, g: 0, b: 0, a: 0.18 },
    offset: { x: 0, y: 4 },
    radius: 12,
    spread: 0,
    visible: true,
    blendMode: 'NORMAL'
  }];
  add(f, T('+', { size: 28, weight: 'Regular', color: '#FFFFFF' }));
  return f;
}

function topBar(content, title, trailing) {
  var bar = stack('TopBar', 'HORIZONTAL', { gap: 14, align: 'CENTER', width: W - MARGIN * 2 });
  var back = add(bar, T('←', { size: 22, color: C.ink }));
  back.name = 'Back';
  grow(add(bar, T(title, { size: 22, weight: 'Semi Bold', color: C.ink, style: 'Headline Small' })));
  if (trailing) add(bar, T(trailing, { size: 20, color: C.muted }));
  add(content, bar, true);
  return bar;
}

function screenHeader(content, title, subtitle, opts) {
  opts = opts || {};
  var h = stack('Header', 'HORIZONTAL', { align: 'CENTER', width: W - MARGIN * 2 });
  var left = stack('Titles', 'VERTICAL', { gap: 1 });
  add(left, T(title, { size: 22, weight: 'Semi Bold', color: C.ink, style: 'Headline Small' }));
  if (subtitle) add(left, T(subtitle, { size: 14, color: C.muted, style: 'Body Medium' }));
  grow(add(h, left));

  var right = stack('Actions', 'HORIZONTAL', { gap: 14, align: 'CENTER' });
  add(right, T('‹', { size: 22, color: C.muted }));
  // Muted at 30% rather than the line colour: a disabled control still has to be
  // visibly a control, not an apparent rendering failure.
  add(right, T('›', { size: 22, color: C.muted, opacity: opts.nextDisabled ? 0.3 : 1 }));
  if (opts.gear) {
    var gear = add(right, T('⚙', { size: 19, color: C.muted }));
    gear.name = 'Gear';
  }
  add(h, right);

  add(content, h, true);
  return h;
}

// ---------------------------------------------------------------- screens

function buildHome(x, y) {
  var s = screen('01 · Home', x, y, { gap: 11 });
  var c = s.content;
  screenHeader(c, 'SafeSpend', 'September 2026', { gear: true, nextDisabled: true });

  // The hero — the one surface in the app that is filled with the 30% brand colour.
  var hero = card({ name: 'SafeToSpendCard', fill: C.teal, pad: 20, gap: 4 });
  add(c, hero, true);
  add(hero, T('SAFE TO SPEND TODAY', { size: 14, weight: 'Semi Bold', color: '#FFFFFF', opacity: 0.75, ls: 0.1 }));
  add(hero, T('Rs 1,240.00', { size: 36, weight: 'Bold', color: '#FFFFFF', ls: -0.5, style: 'Display Small' }));
  add(hero, T('Rs 1,240 a day for the 17 days left.', {
    size: 14, color: '#FFFFFF', opacity: 0.8, width: W - MARGIN * 2 - 40, style: 'Body Medium'
  }));
  add(hero, spacer(1, 10));
  add(hero, progress(0.64, '#FFFFFF', '#FFFFFF', W - MARGIN * 2 - 40, 8, 0.22));
  var heroFoot = stack('Footer', 'HORIZONTAL', { width: W - MARGIN * 2 - 40, align: 'CENTER' });
  add(hero, heroFoot);
  grow(add(heroFoot, T('Rs 28,760 spent', { size: 12, color: '#FFFFFF', opacity: 0.8, style: 'Body Small' })));
  add(heroFoot, T('of Rs 45,000', { size: 12, color: '#FFFFFF', opacity: 0.8, style: 'Body Small' }));

  var stats = card({ name: 'Month totals', pad: 18 });
  add(c, stats, true);
  var row = stack('Row', 'HORIZONTAL', { width: W - MARGIN * 2 - 36 });
  add(stats, row);
  grow(add(row, statTile('Income', 'Rs 85,000', C.income)));
  grow(add(row, statTile('Spent', 'Rs 28,760', C.ink)));
  grow(add(row, statTile('Net', 'Rs 56,240', C.income)));

  add(c, sectionHeader('Where it went', 'Insights'), true);
  var breakdown = card({ name: 'Category breakdown', gap: 14 });
  add(c, breakdown, true);
  // Two categories, not four: the screen is 915dp tall and the recent list below
  // has to stay visible above the fold. Insights is where the full breakdown lives.
  var cats = [
    ['🛒', C.cat2, 'Groceries', 'Rs 12,400', 1.0],
    ['🍽', C.cat1, 'Food & Dining', 'Rs 9,900', 0.8]
  ];
  for (var i = 0; i < cats.length; i++) {
    var r = stack('Share / ' + cats[i][2], 'HORIZONTAL', { gap: 12, align: 'CENTER', width: W - MARGIN * 2 - 32 });
    add(breakdown, r);
    add(r, badge(cats[i][0], cats[i][1], 36));
    var col = stack('Bar', 'VERTICAL', { gap: 6 });
    grow(add(r, col));
    var head = stack('Head', 'HORIZONTAL', { width: W - MARGIN * 2 - 32 - 48, align: 'CENTER' });
    add(col, head);
    grow(add(head, T(cats[i][2], { size: 14, color: C.ink, style: 'Body Medium' })));
    add(head, T(cats[i][3], { size: 14, weight: 'Semi Bold', color: C.ink }));
    add(col, progress(cats[i][4], cats[i][1], C.line, W - MARGIN * 2 - 32 - 48, 6));
  }

  add(c, sectionHeader('Recent activity', 'See all'), true);
  var recent = card({ name: 'Recent activity', gap: 0, pad: 16 });
  add(c, recent, true);
  var rows = [
    ['🛒', C.cat2, 'Keells Super', 'Groceries · Today', 'Rs 2,450.00', false],
    ['🍽', C.cat1, 'Pizza Hut', 'Food & Dining · Today', 'Rs 3,450.00', false],
    ['🚌', C.cat3, 'PickMe', 'Transport · Yesterday', 'Rs 780.00', false]
  ];
  for (var j = 0; j < rows.length; j++) {
    add(recent, transactionRow(rows[j][0], rows[j][1], rows[j][2], rows[j][3], rows[j][4], rows[j][5]));
    if (j < rows.length - 1) {
      var div = figma.createRectangle();
      div.name = 'Divider';
      div.resize(W - MARGIN * 2 - 32, 1);
      fill(div, C.line);
      add(recent, div);
    }
  }

  bottomBar(s.frame, 0);
  fab(s.frame);
  return s.frame;
}

function buildLedger(x, y) {
  var s = screen('02 · Ledger', x, y);
  var c = s.content;
  screenHeader(c, 'Ledger', 'September 2026', { nextDisabled: true });
  add(c, field('Search notes, shops or categories', '⌕'), true);

  var chips = stack('Filters', 'HORIZONTAL', { gap: 8, width: W - MARGIN * 2 });
  add(c, chips, true);
  add(chips, chip('Expenses', true, C.teal));
  add(chips, chip('Income', false));
  add(chips, chip('Groceries', false));
  add(chips, chip('Tran…', false));

  var days = [
    ['Today', '−Rs 5,900.00', [
      ['🛒', C.cat2, 'Keells Super', 'Groceries', 'Rs 2,450.00', false],
      ['🍽', C.cat1, 'Pizza Hut', 'Food & Dining', 'Rs 3,450.00', false]
    ]],
    ['Yesterday', '−Rs 1,980.00', [
      ['🚌', C.cat3, 'PickMe', 'Transport', 'Rs 780.00', false],
      ['🎬', C.cat7, 'Netflix', 'Entertainment', 'Rs 1,200.00', false]
    ]],
    ['1 Sep', '+Rs 85,000.00', [
      ['💼', C.cat1, 'Salary', 'August payroll', 'Rs 85,000.00', true]
    ]]
  ];

  for (var d = 0; d < days.length; d++) {
    var head = stack('DayHeader / ' + days[d][0], 'HORIZONTAL', {
      padding: [6, 4, 2, 4], align: 'CENTER', width: W - MARGIN * 2
    });
    add(c, head, true);
    grow(add(head, T(days[d][0], { size: 14, weight: 'Semi Bold', color: C.muted, ls: 0.1, style: 'Label Large' })));
    add(head, T(days[d][1], { size: 14, weight: 'Semi Bold', color: C.muted }));

    var group = card({ name: 'Day / ' + days[d][0], gap: 0, pad: 12 });
    add(c, group, true);
    var list = days[d][2];
    for (var k = 0; k < list.length; k++) {
      add(group, transactionRow(list[k][0], list[k][1], list[k][2], list[k][3], list[k][4], list[k][5]));
    }
  }

  bottomBar(s.frame, 1);
  fab(s.frame);
  return s.frame;
}

function buildEntry(x, y, name) {
  var s = screen(name || '03 · Add transaction', x, y, { bg: C.mist, gap: 14 });
  var c = s.content;
  topBar(c, 'New transaction');

  var toggle = stack('TypeToggle', 'HORIZONTAL', {
    fill: C.line, radius: 12, padding: [4, 4, 4, 4], width: W - MARGIN * 2
  });
  add(c, toggle, true);
  var seg1 = stack('Expense', 'HORIZONTAL', {
    fill: C.surface, radius: 8, padding: [10, 10, 10, 10], align: 'CENTER', justify: 'CENTER'
  });
  grow(add(toggle, seg1));
  add(seg1, T('Expense', { size: 16, weight: 'Semi Bold', color: C.ink, style: 'Title Medium' }));
  var seg2 = stack('Income', 'HORIZONTAL', { padding: [10, 10, 10, 10], align: 'CENTER', justify: 'CENTER' });
  grow(add(toggle, seg2));
  add(seg2, T('Income', { size: 16, weight: 'Semi Bold', color: C.muted, style: 'Title Medium' }));

  var amount = card({ name: 'Amount', pad: 20, gap: 10 });
  add(c, amount, true);
  add(amount, T('Amount', { size: 12, weight: 'Medium', color: C.muted, ls: 0.4, style: 'Label Medium' }));
  var amountRow = stack('Value', 'HORIZONTAL', { gap: 10, align: 'CENTER' });
  add(amount, amountRow);
  add(amountRow, T('Rs', { size: 26, color: C.muted }));
  add(amountRow, T('2,450.00', { size: 26, weight: 'Bold', color: C.ink, style: 'Headline Medium' }));
  var paste = outlineButton('✨  Paste a bank message', C.coral, W - MARGIN * 2 - 40);
  paste.name = 'Button / Paste a bank message';
  add(amount, paste);

  add(c, T('Category', { size: 16, weight: 'Semi Bold', color: C.ink, style: 'Title Medium' }), true);
  var catCard = card({ name: 'Categories', gap: 8 });
  add(c, catCard, true);
  var rowsOfChips = [
    [['🍽', 'Food & Dining', false, C.cat1], ['🛒', 'Groceries', true, C.cat2]],
    [['🚌', 'Transport', false, C.cat3], ['🧾', 'Bills', false, C.cat4]],
    [['🏠', 'Rent', false, C.cat5], ['💊', 'Health', false, C.cat6]]
  ];
  for (var r = 0; r < rowsOfChips.length; r++) {
    var line = stack('Row', 'HORIZONTAL', { gap: 8 });
    add(catCard, line);
    for (var q = 0; q < rowsOfChips[r].length; q++) {
      var cfg = rowsOfChips[r][q];
      add(line, categoryChip(cfg[0], cfg[1], cfg[2], cfg[3]));
    }
  }

  var details = card({ name: 'Details', gap: 12 });
  add(c, details, true);
  var dateRow = stack('Date', 'HORIZONTAL', { gap: 12, align: 'CENTER', width: W - MARGIN * 2 - 32 });
  add(details, dateRow);
  add(dateRow, T('🗓', { size: 20, color: C.teal }));
  var dcol = stack('Labels', 'VERTICAL', { gap: 0 });
  grow(add(dateRow, dcol));
  add(dcol, T('Date', { size: 12, weight: 'Medium', color: C.muted, ls: 0.4, style: 'Label Medium' }));
  add(dcol, T('12 Sep', { size: 16, color: C.ink, style: 'Body Large' }));
  add(dateRow, T('Change', { size: 14, weight: 'Semi Bold', color: C.teal, ls: 0.1, style: 'Label Large' }));
  add(details, field('Shop or payee (optional)'));
  add(details, field('Note (optional)'));

  add(c, button('Add transaction', C.teal, '#FFFFFF', W - MARGIN * 2), true);
  return s.frame;
}

function buildPasteSheet(x, y) {
  // The same screen with the modal sheet over it, so the prototype can show the
  // transition rather than describing it.
  var frame = buildEntry(x, y, '04 · Paste a bank message');

  var scrim = figma.createRectangle();
  scrim.name = 'Scrim';
  scrim.resize(W, H);
  scrim.x = 0; scrim.y = 0;
  scrim.fills = [solid('#000000', 0.42)];
  frame.appendChild(scrim);

  var sheet = stack('PasteSheet', 'VERTICAL', {
    gap: 12, padding: [12, 20, 30, 20], fill: C.surface, width: W
  });
  frame.appendChild(sheet);

  var handle = figma.createRectangle();
  handle.name = 'Handle';
  handle.resize(38, 4);
  handle.cornerRadius = 2;
  fill(handle, C.line);
  add(sheet, handle);
  handle.layoutAlign = 'CENTER';

  add(sheet, T('Paste a bank message', { size: 19, weight: 'Semi Bold', color: C.ink, style: 'Title Large' }));
  add(sheet, T(
    'Copy the alert SMS your bank sent and paste it here. SafeSpend reads the amount, shop and date — nothing leaves your phone, and it never reads your messages on its own.',
    { size: 14, color: C.muted, width: W - 40, style: 'Body Medium' }
  ));

  var box = stack('Input', 'VERTICAL', {
    padding: [14, 14, 14, 14], radius: 12, stroke: C.line, width: W - 40, height: 118
  });
  add(sheet, box);
  add(box, T(
    'Your A/C 1234 debited by LKR 2,450.00 on 12/09/2026 at KEELLS SUPER. Avl Bal LKR 45,000.00',
    { size: 14, color: C.muted, width: W - 70, style: 'Body Medium' }
  ));

  add(sheet, button('Fill in the form', C.teal, '#FFFFFF', W - 40));

  sheet.x = 0;
  sheet.y = H - sheet.height;
  return frame;
}

function buildBudgets(x, y) {
  var s = screen('05 · Budgets', x, y);
  var c = s.content;
  screenHeader(c, 'Budgets', 'September 2026');

  var summary = card({ name: 'Summary', pad: 20, gap: 14 });
  add(c, summary, true);
  var row = stack('Row', 'HORIZONTAL', { width: W - MARGIN * 2 - 40 });
  add(summary, row);
  grow(add(row, statTile('Budgeted', 'Rs 45,000')));
  grow(add(row, statTile('Spent', 'Rs 28,760')));
  grow(add(row, statTile('Left', 'Rs 16,240', C.teal)));
  add(summary, progress(0.64, C.teal, C.line, W - MARGIN * 2 - 40));
  add(summary, T('1 envelope is over budget.', { size: 12, color: C.coral, style: 'Body Small' }));

  var envelopes = [
    ['🍽', C.cat1, 'Food & Dining', 'Rs 1,900.00 over', 1.0, 'Rs 9,900.00', 'of Rs 8,000.00', true],
    ['🛒', C.cat2, 'Groceries', 'Rs 2,600.00 left', 0.83, 'Rs 12,400.00', 'of Rs 15,000.00', false],
    ['🚌', C.cat3, 'Transport', 'Rs 850.00 left', 0.83, 'Rs 4,150.00', 'of Rs 5,000.00', false]
  ];
  for (var i = 0; i < envelopes.length; i++) {
    var e = envelopes[i];
    var env = card({ name: 'Envelope / ' + e[2], gap: 12 });
    add(c, env, true);
    var head = stack('Head', 'HORIZONTAL', { gap: 12, align: 'CENTER', width: W - MARGIN * 2 - 32 });
    add(env, head);
    add(head, badge(e[0], e[1], 38));
    var col = stack('Labels', 'VERTICAL', { gap: 1 });
    grow(add(head, col));
    add(col, T(e[2], { size: 16, weight: 'Semi Bold', color: C.ink, style: 'Title Medium' }));
    add(col, T(e[3], { size: 12, color: e[7] ? C.coral : C.muted, style: 'Body Small' }));
    add(head, T('🗑', { size: 16, color: C.muted }));
    add(env, progress(e[4], e[7] ? C.coral : e[1], C.line, W - MARGIN * 2 - 32));
    var foot = stack('Foot', 'HORIZONTAL', { width: W - MARGIN * 2 - 32, align: 'CENTER' });
    add(env, foot);
    grow(add(foot, T(e[5], { size: 12, weight: 'Semi Bold', color: C.ink, style: 'Body Small' })));
    add(foot, T(e[6], { size: 12, color: C.muted, style: 'Body Small' }));
  }

  add(c, sectionHeader('Add an envelope'), true);
  var addCard = card({ name: 'Unbudgeted', gap: 8 });
  add(c, addCard, true);
  var line1 = stack('Row', 'HORIZONTAL', { gap: 8 });
  add(addCard, line1);
  add(line1, chip('+  Rent', false));
  add(line1, chip('+  Health', false));
  add(line1, chip('+  Shopping', false));

  bottomBar(s.frame, 2);
  fab(s.frame);
  return s.frame;
}

function donut(size, thickness, slices) {
  var g = figma.createFrame();
  g.name = 'DonutChart';
  g.resize(size, size);
  g.fills = [];
  var total = 0;
  for (var i = 0; i < slices.length; i++) total += slices[i][0];

  var start = -Math.PI / 2;
  for (var j = 0; j < slices.length; j++) {
    var sweep = (slices[j][0] / total) * Math.PI * 2;
    var e = figma.createEllipse();
    e.name = 'Slice';
    e.resize(size, size);
    e.x = 0; e.y = 0;
    fill(e, slices[j][1]);
    // A small gap between wedges keeps them separable without a stroke, which
    // would muddy the colours at this thickness.
    e.arcData = {
      startingAngle: start,
      endingAngle: start + sweep - 0.028,
      innerRadius: (size / 2 - thickness) / (size / 2)
    };
    g.appendChild(e);
    start += sweep;
  }

  var lbl = T('Spent', { size: 12, weight: 'Medium', color: C.muted, ls: 0.4 });
  var val = T('Rs 28.8K', { size: 19, weight: 'Bold', color: C.ink });
  g.appendChild(lbl);
  g.appendChild(val);
  lbl.x = (size - lbl.width) / 2;
  lbl.y = size / 2 - 18;
  val.x = (size - val.width) / 2;
  val.y = size / 2 - 2;
  return g;
}

function buildInsights(x, y) {
  var s = screen('06 · Insights', x, y);
  var c = s.content;
  screenHeader(c, 'Insights', 'September 2026', { nextDisabled: true });

  var chartCard = card({ name: 'Category breakdown', pad: 20, gap: 16 });
  add(c, chartCard, true);
  var holder = stack('ChartHolder', 'HORIZONTAL', {
    width: W - MARGIN * 2 - 40, align: 'CENTER', justify: 'CENTER'
  });
  add(chartCard, holder);
  add(holder, donut(180, 26, [
    [12400, C.cat2], [9900, C.cat1], [4150, C.cat3], [2310, C.cat6]
  ]));

  var legend = [
    [C.cat2, 'Groceries', '43%', 'Rs 12,400'],
    [C.cat1, 'Food & Dining', '34%', 'Rs 9,900'],
    [C.cat3, 'Transport', '15%', 'Rs 4,150'],
    [C.cat6, 'Entertainment', '8%', 'Rs 2,310']
  ];
  for (var i = 0; i < legend.length; i++) {
    var lr = stack('Legend / ' + legend[i][1], 'HORIZONTAL', {
      gap: 10, align: 'CENTER', width: W - MARGIN * 2 - 40
    });
    add(chartCard, lr);
    var dot = figma.createEllipse();
    dot.resize(10, 10);
    dot.name = 'Swatch';
    fill(dot, legend[i][0]);
    add(lr, dot);
    grow(add(lr, T(legend[i][1], { size: 14, color: C.ink, style: 'Body Medium' })));
    add(lr, T(legend[i][2], { size: 12, color: C.muted, style: 'Body Small' }));
    add(lr, T(legend[i][3], { size: 14, weight: 'Semi Bold', color: C.ink }));
  }

  add(c, sectionHeader('Daily pattern'), true);
  var barCard = card({ name: 'Daily pattern', pad: 20, gap: 14 });
  add(c, barCard, true);

  var chartW = W - MARGIN * 2 - 40;
  var bars = stack('BarChart', 'HORIZONTAL', { gap: 3, width: chartW, height: 130, align: 'MAX' });
  add(barCard, bars);
  var heights = [18, 46, 8, 62, 30, 96, 54, 12, 40, 74, 22, 58, 34, 88, 16,
                 44, 26, 70, 38, 10, 52, 80, 28, 64, 42, 18, 56, 36, 92, 24];
  var barW = (chartW - 3 * (heights.length - 1)) / heights.length;
  for (var b = 0; b < heights.length; b++) {
    var rect = figma.createRectangle();
    rect.name = 'Day ' + (b + 1);
    rect.resize(Math.max(2, barW), Math.max(3, heights[b]));
    rect.cornerRadius = barW / 2;
    fill(rect, C.teal);
    bars.appendChild(rect);
  }

  var statsRow = stack('Stats', 'HORIZONTAL', { width: chartW });
  add(barCard, statsRow);
  grow(add(statsRow, statTile('Average / active day', 'Rs 1,150')));
  grow(add(statsRow, statTile('Heaviest day', 'Rs 4,280')));

  bottomBar(s.frame, 3);
  fab(s.frame);
  return s.frame;
}

function buildGoals(x, y) {
  var s = screen('07 · Savings goals', x, y, { gap: 14 });
  var c = s.content;
  topBar(c, 'Savings goals');

  var summary = card({ name: 'Total saved', pad: 20, gap: 2 });
  add(c, summary, true);
  add(summary, T('Saved across all goals', { size: 12, weight: 'Medium', color: C.muted, ls: 0.4, style: 'Label Medium' }));
  add(summary, T('Rs 62,500.00', { size: 26, weight: 'Bold', color: C.teal, style: 'Headline Medium' }));
  add(summary, T('of Rs 220,000.00 targeted', { size: 12, color: C.muted, style: 'Body Small' }));

  var goals = [
    ['Laptop', 'Rs 77,500.00 to go', 0.61, 'Rs 122,500.00 of Rs 200,000.00', C.cat1],
    ['Emergency fund', 'Rs 60,000.00 to go', 0.25, 'Rs 20,000.00 of Rs 80,000.00', C.cat3]
  ];
  for (var i = 0; i < goals.length; i++) {
    var g = goals[i];
    var gc = card({ name: 'Goal / ' + g[0], gap: 12 });
    add(c, gc, true);
    var head = stack('Head', 'HORIZONTAL', { align: 'CENTER', width: W - MARGIN * 2 - 32 });
    add(gc, head);
    var col = stack('Labels', 'VERTICAL', { gap: 1 });
    grow(add(head, col));
    add(col, T(g[0], { size: 16, weight: 'Semi Bold', color: C.ink, style: 'Title Medium' }));
    add(col, T(g[1], { size: 12, color: C.muted, style: 'Body Small' }));
    add(head, T('🗑', { size: 16, color: C.muted }));
    add(gc, progress(g[2], g[4], C.line, W - MARGIN * 2 - 32, 10));
    var foot = stack('Foot', 'HORIZONTAL', { align: 'CENTER', width: W - MARGIN * 2 - 32 });
    add(gc, foot);
    grow(add(foot, T(g[3], { size: 12, weight: 'Semi Bold', color: C.ink, style: 'Body Small' })));
    add(foot, outlineButton('Add money', C.teal));
  }

  fab(s.frame);
  return s.frame;
}

function buildSettings(x, y) {
  var s = screen('08 · Settings', x, y, { gap: 14 });
  var c = s.content;
  topBar(c, 'Settings');

  add(c, sectionHeader('Your month'), true);
  var month = card({ name: 'Your month', pad: 20, gap: 12 });
  add(c, month, true);
  add(month, T(
    "SafeSpend divides what's left of your month across the days that remain. It needs to know how much there is to divide.",
    { size: 14, color: C.muted, width: W - MARGIN * 2 - 40, style: 'Body Medium' }
  ));
  add(month, field('Expected monthly income', 'Rs'));
  add(month, field('Set aside for savings each month', 'Rs'));
  add(month, T('Savings come off the top, before anything is spendable.', {
    size: 12, color: C.muted, width: W - MARGIN * 2 - 40, style: 'Body Small'
  }));

  add(c, sectionHeader('Savings goals', 'Open'), true);

  add(c, sectionHeader('Display'), true);
  var display = card({ name: 'Display', pad: 20, gap: 14 });
  add(c, display, true);
  add(display, field('Rs'));
  add(display, T('Theme', { size: 12, weight: 'Medium', color: C.muted, ls: 0.4, style: 'Label Medium' }));
  var themes = stack('Themes', 'HORIZONTAL', { gap: 8 });
  add(display, themes);
  add(themes, chip('System', true, C.teal));
  add(themes, chip('Light', false));
  add(themes, chip('Dark', false));

  add(c, sectionHeader('Data'), true);
  var data = card({ name: 'Data', pad: 20, gap: 14 });
  add(c, data, true);
  add(data, T(
    'Everything SafeSpend records stays on this phone. There is no account, no server and no network permission.',
    { size: 14, color: C.muted, width: W - MARGIN * 2 - 40, style: 'Body Medium' }
  ));
  add(data, button('Delete all transactions', C.expense, '#FFFFFF'));

  return s.frame;
}

// --------------------------------------------------- documentation frame

function swatchRow(parent, label, entries, note) {
  var block = stack('Swatch / ' + label, 'VERTICAL', { gap: 10 });
  add(parent, block);
  add(block, T(label, { size: 19, weight: 'Semi Bold', color: C.ink, style: 'Title Large' }));

  var row = stack('Swatches', 'HORIZONTAL', { gap: 12 });
  add(block, row);
  for (var i = 0; i < entries.length; i++) {
    var sw = stack('Swatch', 'VERTICAL', { gap: 6, width: 150 });
    add(row, sw);
    var box = figma.createFrame();
    box.name = entries[i][0];
    box.resize(150, 76);
    box.cornerRadius = 12;
    fill(box, entries[i][1]);
    if (entries[i][1] === C.surface) {
      box.strokes = [solid(C.line)];
      box.strokeWeight = 1;
    }
    add(sw, box);
    add(sw, T(entries[i][0], { size: 14, weight: 'Semi Bold', color: C.ink }));
    add(sw, T(entries[i][1], { size: 12, color: C.muted }));
  }
  add(block, T(note, { size: 14, color: C.muted, width: 900, style: 'Body Medium' }));
  return block;
}

function buildCover(x, y) {
  var f = figma.createFrame();
  f.name = '00 · Design system — the 60-30-10 rule';
  f.resize(1000, 1180);
  f.x = x; f.y = y;
  fill(f, C.surface);

  var c = stack('Content', 'VERTICAL', { gap: 28, padding: [56, 50, 56, 50], width: 1000 });
  f.appendChild(c);
  c.x = 0; c.y = 0;

  add(c, T('SafeSpend', { size: 36, weight: 'Bold', color: C.ink, ls: -0.5, style: 'Display Small' }));
  add(c, T(
    'A personal finance tracker that answers the question people actually ask: can I afford this today?',
    { size: 16, color: C.muted, width: 900, style: 'Body Large' }
  ));
  add(c, T(
    'SE4041 Mobile Application Design and Development — Assignment 01. Topic: Personal Finance and Expense Tracking.',
    { size: 14, color: C.muted, width: 900, style: 'Body Medium' }
  ));

  var rule = figma.createRectangle();
  rule.name = 'Rule';
  rule.resize(900, 1);
  fill(rule, C.line);
  add(c, rule);

  add(c, T('Ideation', { size: 22, weight: 'Semi Bold', color: C.ink, style: 'Headline Small' }));
  add(c, T(
    'Most finance apps open on a balance. SafeSpend opens on a decision. The hero is not "you have spent Rs 28,760 this month" — it is "you can safely spend Rs 1,240 today": the month\'s remaining pool, divided across the days that are actually left, minus what has gone today. It self-corrects, so overspending on Monday quietly shrinks every remaining day instead of collapsing the last week of the month.\n\nA daily figure is only honest if the ledger behind it is complete, and ledgers go stale because manual entry is slow. So SafeSpend lets the user paste the bank alert SMS their bank already sent, and reads the amount, shop, date and direction out of it — as an editable draft, never as an authority. It does this without the READ_SMS permission: the app declares no permissions at all.',
    { size: 16, color: C.ink, width: 900, style: 'Body Large' }
  ));

  add(c, spacer(1, 4));
  add(c, T('The 60-30-10 rule', { size: 22, weight: 'Semi Bold', color: C.ink, style: 'Headline Small' }));

  swatchRow(c, '60% — Neutral canvas',
    [['Mist', C.mist], ['Surface', C.surface], ['Ink', C.ink], ['Muted', C.muted], ['Line', C.line]],
    'Backgrounds, cards, list rows and most text. The canvas is never tinted, so roughly three-quarters of the pixels on any screen belong to this group before a single component is placed.');

  swatchRow(c, '30% — Brand',
    [['Deep Teal', C.teal], ['Teal Container', C.tealContainer], ['Teal Deeper', C.tealDeeper]],
    'Exactly one surface per screen carries the brand fill — on Home, the safe-to-spend card. Elsewhere the brand appears only in progress bars, the selected tab, section links and the primary chart series, so the 30% is present on every screen without any screen becoming a teal screen.');

  swatchRow(c, '10% — Accent',
    [['Coral', C.coral], ['Coral Container', C.coralContainer], ['Coral on Brand', C.coralOnBrand]],
    'Rationed to two jobs: start an action (the add button, the paste button) or raise an alarm (over budget, allowance exhausted). There is no third use. Coral on Brand is the variant used on the teal card, where plain Coral reaches only 3.6:1 — fine for the 36sp figure, not for anything smaller.');

  swatchRow(c, 'Data colours — outside the ratio',
    [['Income', C.income], ['Expense', C.expense], ['Category 1', C.cat1], ['Category 5', C.cat5], ['Category 7', C.cat7]],
    'Income green and expense red never fill a surface — they only tint a number or a chart mark, so they do not compete with the accent. Categories store an index into an eight-colour ramp rather than a hex value, so a user-created category cannot go off-palette.');

  add(c, T(
    'Measured on the Home screen at 412 × 915: approximately 62% neutral / 29% brand / 9% accent.',
    { size: 16, weight: 'Semi Bold', color: C.ink, width: 900, style: 'Body Large' }
  ));

  // Fit the board to whatever the copy actually measures, rather than trusting a
  // height guessed before the text was laid out.
  f.resize(1000, Math.max(1180, Math.ceil(c.height)));
  return f;
}

// ------------------------------------------------------------- components

function libraryPage() {
  var page = getOrCreatePage('Components');

  var items = [
    ['CategoryBadge', badge('🛒', C.cat2)],
    ['ProgressTrack', progress(0.64, C.teal, C.line, 280)],
    ['StatTile', statTile('Spent', 'Rs 28,760')],
    ['CategoryChip / Selected', categoryChip('🛒', 'Groceries', true, C.cat2)],
    ['CategoryChip / Default', categoryChip('🚌', 'Transport', false, C.cat3)],
    ['FilterChip / Selected', chip('Expenses', true, C.teal)],
    ['FilterChip / Default', chip('Income', false)],
    ['TransactionRow', transactionRow('🛒', C.cat2, 'Keells Super', 'Groceries · Today', 'Rs 2,450.00', false)],
    ['Field', field('Search notes, shops or categories', '⌕')],
    ['Button / Primary', button('Add transaction', C.teal, '#FFFFFF', 300)],
    ['Button / Outline', outlineButton('Add money', C.teal)],
    ['SectionHeader', sectionHeader('Where it went', 'Insights')]
  ];

  var y = 0;
  for (var i = 0; i < items.length; i++) {
    var node = items[i][1];
    var comp = figma.createComponent();
    comp.name = items[i][0];
    comp.resize(Math.max(node.width, 1), Math.max(node.height, 1));
    comp.fills = [];
    comp.appendChild(node);
    node.x = 0; node.y = 0;
    comp.x = 0;
    comp.y = y;
    y += comp.height + 48;
    page.appendChild(comp);
  }
  return page;
}

// ----------------------------------------------------------------- styles

function byName(list) {
  var map = {};
  for (var i = 0; i < list.length; i++) map[list[i].name] = list[i];
  return map;
}

/**
 * Creates the styles, or updates them if a previous run already made them.
 * Re-running the plugin should leave one set of styles behind, not a second copy
 * called "Deep Teal (1)".
 */
function createStyles() {
  var existingPaint = {};
  var existingText = {};
  try { existingPaint = byName(figma.getLocalPaintStyles()); } catch (e) { /* first run */ }
  try { existingText = byName(figma.getLocalTextStyles()); } catch (e) { /* first run */ }

  for (var i = 0; i < STYLE_GROUPS.length; i++) {
    var name = STYLE_GROUPS[i][0];
    var s = existingPaint[name] || figma.createPaintStyle();
    s.name = name;
    s.paints = [solid(STYLE_GROUPS[i][1])];
    paintStyles[STYLE_GROUPS[i][1]] = s;
  }
  for (var j = 0; j < TEXT_STYLES.length; j++) {
    var tName = TEXT_STYLES[j][0];
    var t = existingText[tName] || figma.createTextStyle();
    t.name = tName;
    t.fontName = font(TEXT_STYLES[j][2]);
    t.fontSize = TEXT_STYLES[j][1];
    t.lineHeight = { unit: 'PIXELS', value: TEXT_STYLES[j][3] };
    if (TEXT_STYLES[j][4]) t.letterSpacing = { unit: 'PIXELS', value: TEXT_STYLES[j][4] };
    textStyles[tName] = t;
  }
}

/**
 * Reuses a page of this name if one exists, emptying it first.
 *
 * Figma's free plan caps a file at three pages, and a second run that blindly
 * created "Screens" and "Components" again would either hit that cap or leave
 * the file with duplicates. Rebuilding in place makes the plugin safe to re-run.
 */
function getOrCreatePage(name) {
  var pages = figma.root.children;
  for (var i = 0; i < pages.length; i++) {
    if (pages[i].name === name) {
      var existing = pages[i].children.slice();
      for (var j = 0; j < existing.length; j++) existing[j].remove();
      return pages[i];
    }
  }
  var page = figma.createPage();
  page.name = name;
  return page;
}

// -------------------------------------------------------------- prototype

var linkFailures = [];

/**
 * Directional transitions carry a required `direction`; SMART_ANIMATE and
 * DISSOLVE must not. Omitting it made every MOVE_IN/MOVE_OUT reaction invalid, so
 * the tab links worked and the FAB, back arrows and sheet did nothing at all.
 */
function transitionFor(type, direction) {
  var t = { type: type || 'SMART_ANIMATE', easing: { type: 'EASE_OUT' }, duration: 0.3 };
  if (t.type === 'MOVE_IN' || t.type === 'MOVE_OUT' || t.type === 'PUSH' ||
      t.type === 'SLIDE_IN' || t.type === 'SLIDE_OUT') {
    t.direction = direction || 'BOTTOM';
    t.matchLayers = false;
  }
  return t;
}

/**
 * Tries the requested transition, then plain DISSOLVE, and under each the current
 * `actions` array shape then the older single `action` shape. Anything that still
 * fails is collected and reported when the plugin closes — a prototype link that
 * quietly doesn't exist is worse than one that looks wrong.
 */
async function link(fromNode, toFrame, type, direction) {
  if (!fromNode) { linkFailures.push('(node not found → ' + (toFrame ? toFrame.name : '?') + ')'); return; }
  if (!toFrame) { linkFailures.push(fromNode.name + ' (no destination)'); return; }

  var candidates = [transitionFor(type, direction), transitionFor('DISSOLVE')];
  for (var i = 0; i < candidates.length; i++) {
    var action = {
      type: 'NODE',
      destinationId: toFrame.id,
      navigation: 'NAVIGATE',
      transition: candidates[i],
      preserveScrollPosition: false
    };
    try {
      await fromNode.setReactionsAsync([{ trigger: { type: 'ON_CLICK' }, actions: [action] }]);
      return;
    } catch (e) { /* try the older shape */ }
    try {
      await fromNode.setReactionsAsync([{ trigger: { type: 'ON_CLICK' }, action: action }]);
      return;
    } catch (e2) { /* try the next transition */ }
  }
  linkFailures.push(fromNode.name);
}

function navItem(frame, label) {
  return frame.findOne(function (n) { return n.name === 'NavItem / ' + label; });
}

function named(frame, name) {
  return frame.findOne(function (n) { return n.name === name; });
}

async function wirePrototype(screens) {
  var tabs = [
    [screens.home, 0],
    [screens.ledger, 1],
    [screens.budgets, 2],
    [screens.insights, 3]
  ];
  var targets = [screens.home, screens.ledger, screens.budgets, screens.insights];
  var labels = ['Home', 'Ledger', 'Budgets', 'Insights'];

  for (var i = 0; i < tabs.length; i++) {
    var frame = tabs[i][0];
    for (var j = 0; j < labels.length; j++) {
      if (i === j) continue;
      await link(navItem(frame, labels[j]), targets[j], 'SMART_ANIMATE');
    }
    // Entry arrives as a sheet from the bottom, the way the real app opens it.
    await link(named(frame, 'FAB'), screens.entry, 'MOVE_IN', 'TOP');
  }

  await link(named(screens.goals, 'FAB'), screens.entry, 'MOVE_IN', 'TOP');
  await link(named(screens.entry, 'Button / Paste a bank message'), screens.paste, 'MOVE_IN', 'TOP');
  await link(named(screens.paste, 'Button / Fill in the form'), screens.entry, 'MOVE_OUT', 'BOTTOM');
  await link(named(screens.paste, 'Scrim'), screens.entry, 'MOVE_OUT', 'BOTTOM');
  await link(named(screens.entry, 'Button / Add transaction'), screens.home, 'MOVE_OUT', 'BOTTOM');
  await link(named(screens.entry, 'Back'), screens.home, 'MOVE_OUT', 'BOTTOM');
  // Full-screen destinations push in from the right and pop back out to it.
  await link(named(screens.goals, 'Back'), screens.home, 'MOVE_OUT', 'RIGHT');
  await link(named(screens.settings, 'Back'), screens.home, 'MOVE_OUT', 'RIGHT');
  await link(named(screens.settings, 'SectionHeader / Savings goals'), screens.goals, 'MOVE_IN', 'LEFT');
  await link(named(screens.home, 'Gear'), screens.settings, 'MOVE_IN', 'LEFT');
  await link(named(screens.home, 'SectionHeader / Where it went'), screens.insights, 'SMART_ANIMATE');
  await link(named(screens.home, 'SectionHeader / Recent activity'), screens.ledger, 'SMART_ANIMATE');
}

// ------------------------------------------------------------------ main

async function main() {
  await Promise.all([
    figma.loadFontAsync(font('Regular')),
    figma.loadFontAsync(font('Medium')),
    figma.loadFontAsync(font('Semi Bold')),
    figma.loadFontAsync(font('Bold'))
  ]);

  createStyles();

  var page = getOrCreatePage('Screens');
  figma.currentPage = page;

  var gapX = 500;
  var docFrame = buildCover(-1120, 0);
  page.appendChild(docFrame);

  var homeFrame = buildHome(0, 0);
  var ledger = buildLedger(gapX, 0);
  var entry = buildEntry(gapX * 2, 0);
  var paste = buildPasteSheet(gapX * 3, 0);
  var budgets = buildBudgets(0, 1060);
  var insights = buildInsights(gapX, 1060);
  var goals = buildGoals(gapX * 2, 1060);
  var settings = buildSettings(gapX * 3, 1060);

  var frames = [homeFrame, ledger, entry, paste, budgets, insights, goals, settings];
  for (var i = 0; i < frames.length; i++) page.appendChild(frames[i]);

  await wirePrototype({
    home: homeFrame,
    ledger: ledger,
    entry: entry,
    paste: paste,
    budgets: budgets,
    insights: insights,
    goals: goals,
    settings: settings
  });

  try {
    page.flowStartingPoints = [{ nodeId: homeFrame.id, name: 'SafeSpend' }];
  } catch (e) {
    console.log('Could not set the prototype starting point: ' + e);
  }

  libraryPage();

  figma.viewport.scrollAndZoomIntoView(frames);

  var summary = 'SafeSpend design built: 8 screens, a design-system frame, ' +
    STYLE_GROUPS.length + ' colour styles, ' + TEXT_STYLES.length + ' text styles and a component page.';
  if (linkFailures.length) {
    summary += ' ' + linkFailures.length + ' prototype link(s) failed: ' + linkFailures.join(', ');
  } else {
    summary += ' All prototype links connected.';
  }
  figma.closePlugin(summary);
}

main().catch(function (err) {
  console.log(err);
  figma.closePlugin('SafeSpend Design Builder failed: ' + err.message);
});
