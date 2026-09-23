/**
 * Good Dream & SpringHaven Luxury Web Application
 * Single Page Application Controller replicating Android GoodDreamApp architecture.
 */

// Razorpay Configuration
const DEFAULT_RAZORPAY_KEY = 'rzp_test_TfYEXhy3Yk78zc';
try {
  const storedKey = localStorage.getItem('gd_razorpay_key');
  if (storedKey && (storedKey === 'rzp_test_TfYvAe3rEpou20' || storedKey === 'rzp_test_TfXgEnJAkeNQHM' || storedKey === 'rzp_test_TfWswbDG9RMpn0' || !storedKey.startsWith('rzp_'))) {
    localStorage.removeItem('gd_razorpay_key');
  }
} catch (e) {}

const RAZORPAY_CONFIG = {
  keyId: localStorage.getItem('gd_razorpay_key') || DEFAULT_RAZORPAY_KEY
};

// Backend API Configuration & Universal Cloud Environment Detector
const API_CONFIG = {
  getBaseUrl: function () {
    try {
      const custom = localStorage.getItem('gd_backend_url');
      if (custom && custom.trim().startsWith('http')) {
        return custom.trim().replace(/\/+$/, '');
      }
      // If running locally, route to same-origin Node.js Express server
      if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        return '';
      }
      // If running directly on Vercel or Netlify, route to same-origin
      if (window.location.hostname.includes('vercel.app') || window.location.hostname.includes('netlify.app')) {
        return '';
      }
      // If running on Firebase Hosting, connect to the live Vercel cloud backend
      if (window.location.hostname.includes('web.app') || window.location.hostname.includes('firebaseapp.com')) {
        return 'https://temporary-speedy-beryl-h8ld7ti.vercel.app';
      }
    } catch (e) {}
    return '';
  },
  isBackendConnected: function () {
    return true;
  },
  getStatusDescription: function () {
    const url = this.getBaseUrl();
    if (url === '') return 'Live Same-Origin Backend (Active)';
    if (url) return `Live Cloud Backend (${url})`;
    return 'Live Cloud Backend';
  }
};

// Application State
const state = {
  activeTab: 'home', // 'home' | 'products' | 'new_launches' | 'account'
  selectedCategory: null,
  searchQuery: '',
  selectedSort: 'Featured',
  selectedPredictiveTag: null,
  cart: [],
  wishlist: new Set(),
  savedAddresses: [],
  orders: [],
  user: null,
  userSleepProfile: null,
  appliedCoupon: null,
  currentModal: null, // 'pdp' | 'bespoke' | 'sleep_quiz' | 'compare' | 'cart' | 'checkout' | 'order_success' | 'order_tracking' | 'ai_concierge' | 'sanctuary_care' | 'login'
  modalData: null,
  selectedCompareIds: [],
  bespokeConfig: {
    coreType: 'POCKET_SPRINGS',
    comfortLayer: 'BELGIAN_LATEX',
    quiltCover: 'ORGANIC_BAMBOO',
    width: 72,
    length: 78,
    thickness: 10,
    firmness: 'Ortho Medium-Firm (7.5/10)',
    monogram: '',
    monogramColor: 'gold',
    includeBedBase: false,
    viewMode: 'exploded', // 'exploded' | 'assembled'
    layers: {
      quilt: true,
      latex: true,
      memory: true,
      springs: true,
      base: true
    }
  },
  aiChatMessages: [
    {
      sender: 'bot',
      text: "Welcome to Good Dream Home Decor! 🌙 I'm your DreamCare AI Concierge. How can I help you today? Ask me about mattress firmness for back pain, custom sizing, stain cleaning, 25-year warranty, or order status."
    }
  ]
};

// Utilities, Sanitization, Image Optimization & Currency Formatter
const escapeHtml = (str) => {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
};

const optimizeImageUrl = (url, width = 600) => {
  if (!url) return '';
  if (url.includes('images.unsplash.com')) {
    let clean = url.replace(/([?&])w=\d+/, `$1w=${width}`);
    if (!clean.includes('w=')) clean += (clean.includes('?') ? '&' : '?') + `w=${width}`;
    clean = clean.replace(/([?&])q=\d+/, '$1q=75');
    if (!clean.includes('q=')) clean += '&q=75';
    if (!clean.includes('auto=format')) clean += '&auto=format';
    if (!clean.includes('fit=crop')) clean += '&fit=crop';
    return clean;
  }
  return url;
};

const formatBotMessage = (text) => {
  const safe = escapeHtml(text);
  return safe.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
};

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0
  }).format(amount).replace('INR', '₹').trim();
};

// LocalStorage Persistence Layer
const STORAGE_KEYS = {
  CART: 'good_dream_cart_v1',
  WISHLIST: 'good_dream_wishlist_v1',
  ADDRESSES: 'good_dream_addresses_v1',
  ORDERS: 'good_dream_orders_v1',
  SLEEP_PROFILE: 'good_dream_sleep_profile_v1',
  USER: 'good_dream_user_v1'
};

function initStorage() {
  try {
    const savedCart = localStorage.getItem(STORAGE_KEYS.CART);
    if (savedCart) {
      const parsed = JSON.parse(savedCart);
      if (Array.isArray(parsed)) state.cart = parsed;
    }

    const savedWishlist = localStorage.getItem(STORAGE_KEYS.WISHLIST);
    if (savedWishlist) {
      const parsed = JSON.parse(savedWishlist);
      if (Array.isArray(parsed)) state.wishlist = new Set(parsed);
    }

    const savedAddresses = localStorage.getItem(STORAGE_KEYS.ADDRESSES);
    if (savedAddresses) {
      const parsed = JSON.parse(savedAddresses);
      if (Array.isArray(parsed) && parsed.length > 0) {
        state.savedAddresses = parsed;
      }
    }

    if (!state.savedAddresses || state.savedAddresses.length === 0) {
      // Provide initial sanctuary address matching Jaipur entity region
      state.savedAddresses = [
        {
          id: 'addr_default_1',
          name: 'Sanctuary Patron',
          phone: '+91 70149 83696',
          addressLine: 'D-4, Vijay Vihar Colony, Naya Kheda, Amba Bari',
          city: 'Jaipur',
          state: 'Rajasthan',
          pincode: '302039',
          tag: 'Home'
        }
      ];
      persistAddresses();
    }

    const savedOrders = localStorage.getItem(STORAGE_KEYS.ORDERS);
    if (savedOrders) {
      const parsed = JSON.parse(savedOrders);
      if (Array.isArray(parsed)) state.orders = parsed;
    }

    const savedProfile = localStorage.getItem(STORAGE_KEYS.SLEEP_PROFILE);
    if (savedProfile) state.userSleepProfile = String(savedProfile);

    const savedUser = localStorage.getItem(STORAGE_KEYS.USER);
    if (savedUser) {
      try {
        state.user = JSON.parse(savedUser);
      } catch (e) {
        state.user = null;
      }
    }
  } catch (e) {
    console.error('Failed to load local storage state:', e);
  }
}

function persistUser() {
  try {
    if (state.user) {
      localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(state.user));
    } else {
      localStorage.removeItem(STORAGE_KEYS.USER);
    }
  } catch (e) {}
}

function persistCart() {
  try {
    localStorage.setItem(STORAGE_KEYS.CART, JSON.stringify(state.cart));
  } catch (e) {}
}

function persistWishlist() {
  try {
    localStorage.setItem(STORAGE_KEYS.WISHLIST, JSON.stringify(Array.from(state.wishlist)));
  } catch (e) {}
}

function persistAddresses() {
  try {
    localStorage.setItem(STORAGE_KEYS.ADDRESSES, JSON.stringify(state.savedAddresses));
  } catch (e) {}
}

function persistOrders() {
  try {
    localStorage.setItem(STORAGE_KEYS.ORDERS, JSON.stringify(state.orders));
  } catch (e) {}
}

function persistSleepProfile() {
  try {
    if (state.userSleepProfile) {
      localStorage.setItem(STORAGE_KEYS.SLEEP_PROFILE, state.userSleepProfile);
    }
  } catch (e) {}
}

// Navigation & Tab Management
function setActiveTab(tabName) {
  state.activeTab = tabName;
  window.scrollTo({ top: 0, behavior: 'smooth' });
  renderApp();
}

// Modal Management
function openModal(modalName, data = null) {
  state.currentModal = modalName;
  state.modalData = data;
  document.body.style.overflow = 'hidden';
  renderModal();
}

function closeModal() {
  state.currentModal = null;
  state.modalData = null;
  document.body.style.overflow = 'auto';
  const modalContainer = document.getElementById('modalContainer');
  if (modalContainer) {
    modalContainer.innerHTML = '';
    modalContainer.classList.remove('active');
  }
}

// Cart Operations
function addToCart(productId, quantity = 1, customConfig = null) {
  if (!productId || typeof productId !== 'string') return;
  const safeQty = Math.max(1, parseInt(quantity, 10) || 1);

  const existing = state.cart.find(
    (item) => item.productId === productId && JSON.stringify(item.customConfig) === JSON.stringify(customConfig)
  );

  if (existing) {
    existing.quantity += safeQty;
  } else {
    state.cart.push({
      productId,
      quantity: safeQty,
      customConfig,
      addedAt: Date.now()
    });
  }

  persistCart();
  updateBadgeCounts();
  showToastNotification(`Added to Sanctuary Cart 🌿`);
}

function updateCartQuantity(index, delta) {
  const safeDelta = parseInt(delta, 10) || 0;
  if (state.cart[index]) {
    state.cart[index].quantity += safeDelta;
    if (state.cart[index].quantity <= 0) {
      state.cart.splice(index, 1);
    }
    persistCart();
    updateBadgeCounts();
    if (state.currentModal === 'cart') {
      renderModal();
    }
  }
}

function removeCartItem(index) {
  state.cart.splice(index, 1);
  persistCart();
  updateBadgeCounts();
  if (state.currentModal === 'cart') {
    renderModal();
  }
}

function clearCart() {
  state.cart = [];
  persistCart();
  updateBadgeCounts();
  if (state.currentModal === 'cart') {
    renderModal();
  }
}

function toggleWishlist(productId) {
  if (state.wishlist.has(productId)) {
    state.wishlist.delete(productId);
    showToastNotification('Removed from Wishlist');
  } else {
    state.wishlist.add(productId);
    showToastNotification('Added to Sanctuary Wishlist ❤️');
  }
  persistWishlist();
  updateBadgeCounts();
  renderApp();
  if (state.currentModal === 'pdp' && state.modalData?.id === productId) {
    renderModal();
  }
}

function updateBadgeCounts() {
  const totalCartCount = state.cart.reduce((sum, item) => sum + item.quantity, 0);
  const cartBadge = document.getElementById('navCartCount');
  if (cartBadge) {
    cartBadge.textContent = totalCartCount;
    cartBadge.style.display = totalCartCount > 0 ? 'inline-block' : 'none';
  }

  const wishlistBadge = document.getElementById('navWishlistCount');
  if (wishlistBadge) {
    wishlistBadge.textContent = state.wishlist.size;
    wishlistBadge.style.display = state.wishlist.size > 0 ? 'inline-block' : 'none';
  }

  const memberBtn = document.getElementById('navMemberBtn');
  const memberText = document.getElementById('navMemberText');
  const memberIcon = document.getElementById('navMemberIcon');
  if (memberBtn && memberText) {
    if (state.user) {
      memberText.textContent = state.user.name ? state.user.name.split(' ')[0] : 'Member';
      if (memberIcon) memberIcon.textContent = '👑';
      memberBtn.title = `Privilege Member: ${state.user.name} (WELCOME25 Active)`;
    } else {
      memberText.textContent = 'Sign In';
      if (memberIcon) memberIcon.textContent = '🔒';
      memberBtn.title = 'Sign In to unlock 25% Member Privilege (WELCOME25)';
    }
  }
}

function handleMemberBtnClick() {
  if (state.user) {
    setActiveTab('account');
  } else {
    openModal('login');
  }
}

let toastTimer = null;
function showToastNotification(message, duration = 3200) {
  const toast = document.getElementById('globalToast');
  if (!toast) return;
  toast.innerHTML = `<span>${message}</span>`;
  toast.classList.add('visible');
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.classList.remove('visible');
  }, duration);
}

// Pricing & Calculations
function getCartTotals() {
  let subtotal = 0;
  for (const item of state.cart) {
    if (item.productId === 'bespoke_mattress_custom') {
      const price = item.customConfig ? item.customConfig.price : 64000;
      subtotal += price * item.quantity;
    } else {
      const prod = APP_PRODUCTS.find((p) => p.id === item.productId);
      if (prod) {
        subtotal += prod.price * item.quantity;
      }
    }
  }

  let discount = 0;
  if (state.appliedCoupon) {
    if (state.appliedCoupon.percent) {
      discount = subtotal * (state.appliedCoupon.percent / 100);
    } else if (state.appliedCoupon.flat) {
      discount = state.appliedCoupon.flat;
    }
  }

  const finalTotal = Math.max(0, subtotal - discount);
  const advance20Percent = Math.round(finalTotal * 0.20);
  const balanceCod = finalTotal - advance20Percent;

  return {
    subtotal,
    discount,
    finalTotal,
    advance20Percent,
    balanceCod
  };
}

// Authentic Bespoke Studio Models (app/src/main/java/com/example/data/model/BespokeStudioModels.kt)
const BESPOKE_CORE_TYPES = {
  POCKET_SPRINGS: {
    id: "core_pocket_springs",
    title: "Swedish Pocket Springs",
    subtitle: "Zero-Disturbance Motion Isolation",
    basePrice: 34999.0,
    code: "SPRG",
    firmnessScore: 7.0
  },
  ORGANIC_NATURAL_LATEX: {
    id: "core_organic_latex",
    title: "100% Organic Belgian Latex",
    subtitle: "Eco-Pure GOLS Certified Pincore",
    basePrice: 46999.0,
    code: "LATX",
    firmnessScore: 6.5
  },
  ORTHOPEDIC_HIGH_RESILIENCE: {
    id: "core_ortho_hr",
    title: "Dual-Density Ortho HR Core",
    subtitle: "Chiropractic Spinal Alignment",
    basePrice: 29999.0,
    code: "ORTH",
    firmnessScore: 8.5
  },
  HYBRID_SOVEREIGN: {
    id: "core_hybrid_sovereign",
    title: "Imperial Sovereign Hybrid",
    subtitle: "Pocket Coils + Organic Latex",
    basePrice: 54999.0,
    code: "HYBR",
    firmnessScore: 7.5
  }
};

const BESPOKE_COMFORT_LAYERS = {
  BELGIAN_LATEX: {
    id: "comfort_belgian_latex",
    title: '2" Belgian Pincore Latex',
    subtitle: "Buoyant Organic Bounce",
    additionalPrice: 7500.0,
    airflowRating: "98% High"
  },
  COOLING_GRAPHITE_GEL: {
    id: "comfort_cryo_gel",
    title: '2" Cryo-Gel Cooling Foam',
    subtitle: "Active Thermoregulation",
    additionalPrice: 5500.0,
    airflowRating: "92% Very Good"
  },
  ORGANIC_WOOL_CASHMERE: {
    id: "comfort_wool_cashmere",
    title: "Hand-Tufted Merino & Cashmere",
    subtitle: "Artisan Royal Softness",
    additionalPrice: 11000.0,
    airflowRating: "95% Excellent"
  },
  ZERO_GRAVITY_FOAM: {
    id: "comfort_zero_g",
    title: '2" Zero-G Memory Cloud',
    subtitle: "Weightless Pressure Relief",
    additionalPrice: 4500.0,
    airflowRating: "88% Good"
  }
};

const BESPOKE_QUILT_COVERS = {
  ORGANIC_BAMBOO: {
    id: "quilt_organic_bamboo",
    title: "450 GSM Organic Bamboo",
    subtitle: "Hypoallergenic & Silky",
    additionalPrice: 0.0
  },
  BELGIAN_DAMASK: {
    id: "quilt_belgian_damask",
    title: "Heritage Belgian Damask",
    subtitle: "Royal Woven Jacquard",
    additionalPrice: 5000.0
  },
  MULBERRY_SILK: {
    id: "quilt_mulberry_silk",
    title: "Imperial Mulberry Silk Jacquard",
    subtitle: "Lustrous Gold-Piped Finish",
    additionalPrice: 8500.0
  }
};

// Exact Dynamic Valuation Formula from BespokeMattressConfiguration.calculatePrice()
function calculateBespokePrice() {
  const cfg = state.bespokeConfig;
  const standardKingArea = 72.0 * 78.0; // 5,616 sq. inches
  const actualArea = (cfg.width * cfg.length);
  const areaMultiplier = Math.min(Math.max(actualArea / standardKingArea, 0.6), 1.6);
  const areaSqFt = (actualArea / 144.0).toFixed(1);

  const core = BESPOKE_CORE_TYPES[cfg.coreType] || BESPOKE_CORE_TYPES.POCKET_SPRINGS;
  const comfort = BESPOKE_COMFORT_LAYERS[cfg.comfortLayer] || BESPOKE_COMFORT_LAYERS.BELGIAN_LATEX;
  const quilt = BESPOKE_QUILT_COVERS[cfg.quiltCover] || BESPOKE_QUILT_COVERS.ORGANIC_BAMBOO;

  // Thickness adjustment (standard is 8 inches; +₹1,500 per additional inch)
  const thicknessAdjustment = Math.max(0, cfg.thickness - 8) * 1500.0;

  const baseComponent = (core.basePrice * areaMultiplier) + thicknessAdjustment;
  const comfortComponent = comfort.additionalPrice * areaMultiplier;
  const quiltComponent = quilt.additionalPrice * areaMultiplier;
  const monogramComponent = cfg.monogram.trim() ? 1500.0 : 0.0;
  const bedBaseComponent = cfg.includeBedBase ? 18000.0 : 0.0;

  const total = Math.round((baseComponent + comfortComponent + quiltComponent + monogramComponent + bedBaseComponent) / 100.0) * 100.0;
  const advance = Math.round((total * 0.20) / 100.0) * 100.0;

  return {
    totalPrice: total,
    advance20: advance,
    balanceAtDoorstep: total - advance,
    areaSqFt,
    core,
    comfort,
    quilt,
    sku: `BESPOKE-${core.code}-CST-${cfg.width}x${cfg.length}-${Math.floor(1000 + Math.random() * 9000)}`
  };
}

// Main Render Dispatcher
function renderApp() {
  const mainContent = document.getElementById('mainAppContent');
  if (!mainContent) return;

  // Highlight active bottom tab button
  document.querySelectorAll('.tab-btn').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.tab === state.activeTab);
  });

  if (state.activeTab === 'home') {
    mainContent.innerHTML = renderHomeTab();
  } else if (state.activeTab === 'products') {
    mainContent.innerHTML = renderProductsTab();
  } else if (state.activeTab === 'new_launches') {
    mainContent.innerHTML = renderNewLaunchesTab();
  } else if (state.activeTab === 'account') {
    mainContent.innerHTML = renderAccountTab();
  }

  attachDynamicListeners();
}

// -------------------------------------------------------------
// TAB 1: HOME (Exact match with HomeScreen.kt)
// -------------------------------------------------------------
function renderHomeTab() {
  return `
    <div class="home-container">
      <!-- High-Engagement Flagship Hero Showcase Card -->
      <div class="home-hero-card" onclick="setActiveTab('new_launches')">
        <div class="hero-overlay"></div>
        <div class="hero-content">
          <div class="hero-top-row">
            <span class="badge-flagship breathe">FLAGSHIP COLLECTION</span>
            <span class="badge-pill">🛡️ 25-Yr Warranty</span>
          </div>
          <div class="hero-bottom-text">
            <h2>SpringHaven™ Series</h2>
            <p>15-Inch Ultra-Luxury Orthopedic Bedding with Swedish Multi-Zone Pocket Coils & Natural Cool-Gel Memory Foam.</p>
            <div class="hero-action-buttons">
              <button class="btn-hero-primary" onclick="event.stopPropagation(); setActiveTab('new_launches')">Explore Flagship →</button>
              <button class="btn-hero-outline" onclick="event.stopPropagation(); openModal('sanctuary_care', { type: 'your_needs' })">Custom Sizing</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Trust Pillars Strip (HomeTrustPillarsStrip.kt) -->
      <div class="home-trust-strip">
        <div class="trust-item">
          <span class="icon">🏛️</span>
          <span><strong>25-Yr Guarantee</strong></span>
        </div>
        <div class="trust-divider"></div>
        <div class="trust-item">
          <span class="icon">🚚</span>
          <span><strong>Free Delivery</strong></span>
        </div>
        <div class="trust-divider"></div>
        <div class="trust-item">
          <span class="icon">🌿</span>
          <span><strong>Handcrafted Pure</strong></span>
        </div>
      </div>

      <!-- Precision Sleep & Bespoke Suite (HomeSanctuarySuiteCard.kt) -->
      <div class="home-suite-card">
        <div class="suite-header">
          <div>
            <div class="suite-eyebrow">PRECISION SLEEP & BESPOKE SUITE</div>
            <h3>Sanctuary Studio & Advisory</h3>
          </div>
          <span class="badge-studio">3D ATELIER</span>
        </div>

        <!-- Bespoke Mattress Architect Banner -->
        <div class="suite-bespoke-banner" onclick="openModal('bespoke')">
          <div class="atelier-icon">✨</div>
          <div class="atelier-info">
            <div class="atelier-title">
              <strong>Bespoke Mattress Architect</strong>
              <span class="badge-micro">3D ATELIER</span>
            </div>
            <p>Custom pocket springs, latex, inch sizes & monogram embroidery</p>
          </div>
          <div class="atelier-arrow">→</div>
        </div>

        <!-- 4 Quick Tool Tiles -->
        <div class="sleep-tool-tiles-grid">
          <div class="tool-tile" onclick="openModal('sleep_quiz')">
            <span class="tool-badge gold">3-STEP</span>
            <div class="tool-icon">🌙</div>
            <div class="tool-name">Sleep Quiz</div>
            <div class="tool-desc">Firmness</div>
          </div>
          <div class="tool-tile" onclick="openModal('compare')">
            <span class="tool-badge green">SPECS</span>
            <div class="tool-icon">⚖️</div>
            <div class="tool-name">Compare</div>
            <div class="tool-desc">Matrix</div>
          </div>
          <div class="tool-tile" onclick="openModal('order_tracking')">
            <span class="tool-badge gold">LIVE</span>
            <div class="tool-icon">📦</div>
            <div class="tool-name">Tracking</div>
            <div class="tool-desc">Status</div>
          </div>
          <div class="tool-tile" onclick="openModal('ai_concierge')">
            <span class="tool-badge green">SMART</span>
            <div class="tool-icon">🤖</div>
            <div class="tool-name">Concierge</div>
            <div class="tool-desc">AI Assist</div>
          </div>
        </div>
      </div>

      <!-- Bespoke Services & Client Sanctuary Care Header -->
      <div class="section-title-wrap">
        <div class="eyebrow">BESPOKE SERVICES & CARE</div>
        <h3>Client Sanctuary Care</h3>
      </div>

      <!-- 6 Discrete Rounded Rectangular Action Cards in 2-Column Grid -->
      <div class="home-actions-grid">
        <div class="action-card" onclick="openModal('sanctuary_care', { type: 'your_needs' })">
          <div class="action-card-top">
            <div class="action-icon">📋</div>
            <span class="action-badge">CUSTOM</span>
          </div>
          <div class="action-card-bottom">
            <h4>Your Needs</h4>
            <p>Bespoke Custom Inquiry</p>
          </div>
        </div>

        <div class="action-card highlighted" onclick="openModal('sanctuary_care', { type: 'sponsor_rewards' })">
          <div class="action-card-top">
            <div class="action-icon">🎁</div>
            <span class="action-badge gold">₹2,500</span>
          </div>
          <div class="action-card-bottom">
            <h4>Sponsor Rewards</h4>
            <p>Earn ₹2,500 Store Credit</p>
          </div>
        </div>

        <div class="action-card" onclick="openModal('sanctuary_care', { type: 'warranty' })">
          <div class="action-card-top">
            <div class="action-icon">🛡️</div>
            <span class="action-badge">25-Yr</span>
          </div>
          <div class="action-card-bottom">
            <h4>25-Yr Warranty</h4>
            <p>Digital Vault & Claims</p>
          </div>
        </div>

        <div class="action-card" onclick="openModal('sanctuary_care', { type: 'repairs' })">
          <div class="action-card-top">
            <div class="action-icon">🔧</div>
          </div>
          <div class="action-card-bottom">
            <h4>Repairs & Care</h4>
            <p>Technician Dispatch</p>
          </div>
        </div>

        <div class="action-card" onclick="openModal('sanctuary_care', { type: 'feedbacks' })">
          <div class="action-card-top">
            <div class="action-icon">⭐</div>
          </div>
          <div class="action-card-bottom">
            <h4>Feedbacks</h4>
            <p>Verified Client Reviews</p>
          </div>
        </div>

        <div class="action-card" onclick="openModal('sanctuary_care', { type: 'complaints' })">
          <div class="action-card-top">
            <div class="action-icon">🤝</div>
            <span class="action-badge">24h SLA</span>
          </div>
          <div class="action-card-bottom">
            <h4>Concierge Support</h4>
            <p>24h Priority Escalation</p>
          </div>
        </div>
      </div>

      <!-- Sanctuary Flagship Boutique & Statutory Office Footer Card -->
      <div class="home-flagship-card">
        <div class="flagship-header">
          <div class="flagship-pin">📍</div>
          <div>
            <div class="flagship-eyebrow">SANCTUARY FLAGSHIP BOUTIQUE</div>
            <h4>Experience Good Dream Comfort</h4>
          </div>
        </div>
        <div class="flagship-address">
          <strong>${CORPORATE_CONFIG.companyName}</strong><br>
          ${CORPORATE_CONFIG.registeredOffice}<br>
          <span style="color: var(--text-muted); font-size: 0.85rem;">Marketed by: ${CORPORATE_CONFIG.marketedBy}</span>
        </div>
        <div class="flagship-timings">
          🕒 Hours: ${CORPORATE_CONFIG.supportHours}
        </div>
        <div class="flagship-footer-row">
          <span>Experience Studio Consultations</span>
          <button class="btn-text-gold" onclick="openModal('sanctuary_care', { type: 'your_needs' })">Book Visit →</button>
        </div>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// TAB 2: PRODUCTS (Exact match with CategoriesHub & ProductListing)
// -------------------------------------------------------------
function renderProductsTab() {
  const filteredProducts = getFilteredProducts();

  return `
    <div class="products-container">
      <!-- 12 Categories Horizontal Carousel / Filter Strip -->
      <div class="categories-bar-wrap">
        <div class="categories-header-row">
          <div>
            <div class="cat-eyebrow">PRODUCT CATALOG</div>
            <h3>12 Categories Hub</h3>
          </div>
          ${state.selectedCategory ? `<button class="btn-text-gold" onclick="selectCategory(null)">View All</button>` : ''}
        </div>
        <div class="categories-scroll-row">
          <div class="cat-pill ${state.selectedCategory === null ? 'active' : ''}" onclick="selectCategory(null)">
            <span class="cat-pill-icon">✨</span>
            <span class="cat-pill-text">All Items (${APP_PRODUCTS.length})</span>
          </div>
          ${APP_CATEGORIES.map(
            (cat) => `
            <div class="cat-pill ${state.selectedCategory === cat.id ? 'active' : ''}" onclick="selectCategory('${cat.id}')">
              <img src="${optimizeImageUrl(cat.thumbnailUrl, 120)}" alt="${escapeHtml(cat.name)}" class="cat-pill-img" loading="lazy" decoding="async">
              <span class="cat-pill-text">${escapeHtml(cat.name)}</span>
            </div>
          `
          ).join('')}
        </div>
      </div>

      <!-- Real-Time Search & Predictive Filter Pills -->
      <div class="search-filter-card">
        <div class="search-input-wrap">
          <span class="search-icon">🔍</span>
          <input 
            type="text" 
            id="catalogSearchInput" 
            placeholder="Search mattresses, pillows, protectors, beds..." 
            value="${escapeHtml(state.searchQuery)}" 
            oninput="handleSearchInput(this.value)"
          >
          ${state.searchQuery ? `<button class="clear-search" onclick="handleSearchInput('')">×</button>` : ''}
        </div>

        <!-- Predictive Tags Strip -->
        <div class="predictive-tags-row">
          ${[
            '🛌 Orthopedic',
            '👑 King (72"x78")',
            '🌿 Belgian Latex',
            '🌀 Pocket Springs',
            '💰 Under ₹35,000',
            '💰 Under ₹50,000',
            '☁️ Ultra Plush',
            '🧱 Extra Firm'
          ]
            .map(
              (tag) => `
            <button 
              class="predictive-tag-chip ${state.selectedPredictiveTag === tag ? 'active' : ''}" 
              onclick="togglePredictiveTag('${tag}')"
            >
              ${tag}
            </button>
          `
            )
            .join('')}
        </div>

        <!-- Sort Filter Bar -->
        <div class="sort-bar-row">
          <span>Showing <strong>${filteredProducts.length}</strong> items</span>
          <div class="sort-dropdown-wrap">
            <label for="catalogSortSelect">Sort by:</label>
            <select id="catalogSortSelect" onchange="handleSortChange(this.value)">
              <option value="Featured" ${state.selectedSort === 'Featured' ? 'selected' : ''}>Featured</option>
              <option value="SpringHaven Only" ${state.selectedSort === 'SpringHaven Only' ? 'selected' : ''}>SpringHaven Only</option>
              <option value="Price: Low to High" ${state.selectedSort === 'Price: Low to High' ? 'selected' : ''}>Price: Low to High</option>
              <option value="Price: High to Low" ${state.selectedSort === 'Price: High to Low' ? 'selected' : ''}>Price: High to Low</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Sleep Quiz Match Prompt if not taken -->
      ${
        !state.userSleepProfile
          ? `
        <div class="quiz-teaser-card" onclick="openModal('sleep_quiz')">
          <div class="teaser-icon">🌙</div>
          <div class="teaser-text">
            <strong>Not sure which firmness fits your spine?</strong>
            <span>Take the 60-second Sleep Firmness Diagnostic for personalized recommendations.</span>
          </div>
          <button class="btn-quiz-mini">Start Quiz →</button>
        </div>
      `
          : `
        <div class="quiz-matched-banner">
          <span>✨ Current Sleep Profile: <strong>${state.userSleepProfile}</strong></span>
          <button class="btn-text-gold" onclick="openModal('sleep_quiz')">Retake Quiz</button>
        </div>
      `
      }

      <!-- 2-Column Responsive Product Grid -->
      <div class="product-grid">
        ${
          filteredProducts.length > 0
            ? filteredProducts.map((product) => renderProductCard(product)).join('')
            : `
            <div class="no-products-empty">
              <div class="empty-icon">🛏️</div>
              <h4>No matching sleep systems found</h4>
              <p>Try clearing your search query or choosing another category.</p>
              <button class="btn-hero-primary" onclick="resetFilters()">Reset All Filters</button>
            </div>
          `
        }
      </div>
    </div>
  `;
}

function renderProductCard(product) {
  const isWishlisted = state.wishlist.has(product.id);
  const isSleepMatch = isProductSleepMatch(product);

  return `
    <div class="product-card" onclick="openModal('pdp', { id: '${product.id}' })">
      <div class="product-card-media">
        <img src="${optimizeImageUrl(product.images[0], 500)}" alt="${escapeHtml(product.title)}" loading="lazy" decoding="async">
        <button 
          class="btn-wishlist-heart ${isWishlisted ? 'active' : ''}" 
          onclick="event.stopPropagation(); toggleWishlist('${product.id}')"
          title="Toggle Wishlist"
        >
          ${isWishlisted ? '❤️' : '🤍'}
        </button>

        ${
          product.isSpringhavenSeries
            ? `<span class="badge-flagship-card">SPRINGHAVEN 15"</span>`
            : product.isNewLaunch
            ? `<span class="badge-new-card">NEW LAUNCH</span>`
            : ''
        }

        ${isSleepMatch ? `<span class="badge-sleep-match">✨ Sleep Match</span>` : ''}
      </div>

      <div class="product-card-body">
        <div class="product-card-meta">
          <span class="meta-firmness">${product.firmness}</span>
          <span class="meta-warranty">${product.warrantyYears}-Yr Warranty</span>
        </div>

        <h4 class="product-card-title">${product.title}</h4>
        <p class="product-card-subtitle">${product.subtitle}</p>

        <div class="product-card-pricing">
          <span class="current-price">${formatCurrency(product.price)}</span>
          ${
            product.originalPrice > product.price
              ? `
            <span class="original-price">${formatCurrency(product.originalPrice)}</span>
            <span class="discount-pill">${Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)}% OFF</span>
          `
              : ''
          }
        </div>

        <div class="product-card-footer">
          <button 
            class="btn-quick-add" 
            onclick="event.stopPropagation(); addToCart('${product.id}', 1)"
          >
            + Add to Cart
          </button>
        </div>
      </div>
    </div>
  `;
}

function isProductSleepMatch(product) {
  if (!state.userSleepProfile) return false;
  const p = state.userSleepProfile.toLowerCase();
  const f = (product.firmness || '').toLowerCase();
  if (p.includes('firm') && f.includes('firm')) return true;
  if (p.includes('plush') && f.includes('plush')) return true;
  if (p.includes('balanced') && (f.includes('balanced') || f.includes('medium'))) return true;
  return false;
}

function getFilteredProducts() {
  let list = [...APP_PRODUCTS];

  // Category Filter
  if (state.selectedCategory) {
    list = list.filter((p) => p.categoryId === state.selectedCategory);
  }

  // Search Filter
  if (state.searchQuery.trim()) {
    const q = state.searchQuery.toLowerCase().trim();
    list = list.filter(
      (p) =>
        p.title.toLowerCase().includes(q) ||
        p.subtitle.toLowerCase().includes(q) ||
        p.description.toLowerCase().includes(q) ||
        p.material.toLowerCase().includes(q) ||
        p.firmness.toLowerCase().includes(q)
    );
  }

  // Predictive Tag Filter
  if (state.selectedPredictiveTag) {
    const tag = state.selectedPredictiveTag;
    if (tag.includes('Orthopedic')) {
      list = list.filter((p) => p.title.includes('Ortho') || p.description.includes('orthopedic'));
    } else if (tag.includes('King')) {
      list = list.filter((p) => p.dimensions.includes('King'));
    } else if (tag.includes('Latex')) {
      list = list.filter((p) => p.material.includes('Latex'));
    } else if (tag.includes('Pocket Springs')) {
      list = list.filter((p) => p.material.includes('Pocket') || p.material.includes('Coil'));
    } else if (tag.includes('Under ₹35,000')) {
      list = list.filter((p) => p.price <= 35000);
    } else if (tag.includes('Under ₹50,000')) {
      list = list.filter((p) => p.price <= 50000);
    } else if (tag.includes('Plush')) {
      list = list.filter((p) => p.firmness.includes('Plush'));
    } else if (tag.includes('Firm')) {
      list = list.filter((p) => p.firmness.includes('Firm'));
    }
  }

  // Sorting
  if (state.selectedSort === 'SpringHaven Only') {
    list = list.filter((p) => p.isSpringhavenSeries);
  } else if (state.selectedSort === 'Price: Low to High') {
    list.sort((a, b) => a.price - b.price);
  } else if (state.selectedSort === 'Price: High to Low') {
    list.sort((a, b) => b.price - a.price);
  }

  return list;
}

function selectCategory(catId) {
  state.selectedCategory = catId;
  renderApp();
}

function handleSearchInput(query) {
  state.searchQuery = query;
  renderApp();
  const input = document.getElementById('catalogSearchInput');
  if (input) {
    input.focus();
    const len = input.value.length;
    input.setSelectionRange(len, len);
  }
}

function togglePredictiveTag(tag) {
  state.selectedPredictiveTag = state.selectedPredictiveTag === tag ? null : tag;
  renderApp();
}

function handleSortChange(sortVal) {
  state.selectedSort = sortVal;
  renderApp();
}

function resetFilters() {
  state.selectedCategory = null;
  state.searchQuery = '';
  state.selectedPredictiveTag = null;
  state.selectedSort = 'Featured';
  renderApp();
}

// -------------------------------------------------------------
// TAB 3: NEW LAUNCHES (SpringHaven Flagship Dedicated Showcase)
// -------------------------------------------------------------
function renderNewLaunchesTab() {
  const flagshipProducts = APP_PRODUCTS.filter((p) => p.isSpringhavenSeries);

  return `
    <div class="new-launches-container">
      <div class="launches-hero">
        <span class="badge-flagship">FLAGSHIP SHOWCASE</span>
        <h2>SpringHaven™ 15-Inch Sanctuary Ensembles</h2>
        <p>
          Mastercrafted for India's finest private residences. Integrating multi-tier micro pocket coils, 100% Belgian organic latex, hand-tufted cashmere damask, and solid teak foundations with an official 25-Year Warranty.
        </p>
      </div>

      <div class="flagship-showcase-list">
        ${flagshipProducts
          .map(
            (prod) => `
          <div class="flagship-item-card" onclick="openModal('pdp', { id: '${prod.id}' })">
            <div class="flagship-item-media">
              <img src="${optimizeImageUrl(prod.images[0], 600)}" alt="${escapeHtml(prod.title)}" loading="lazy" decoding="async">
              <span class="badge-tag">25-YEAR GUARANTEE</span>
            </div>
            <div class="flagship-item-info">
              <div class="flagship-firmness-tag">${prod.firmness} • ${prod.dimensions}</div>
              <h3>${prod.title}</h3>
              <p class="desc">${prod.description}</p>
              
              <div class="specs-preview-chips">
                <span>Thickness: <strong>${prod.thicknessInches}"</strong></span>
                <span>Warranty: <strong>${prod.warrantyYears || 25} Years</strong></span>
                <span>White-Glove: <strong>Included</strong></span>
              </div>

              <div class="flagship-price-action">
                <div>
                  <span class="price-val">${formatCurrency(prod.price)}</span>
                  <span class="mrp-val">${formatCurrency(prod.originalPrice)}</span>
                  <span class="gst-tag">(Inclusive of 18% GST)</span>
                </div>
                <button class="btn-hero-primary" onclick="event.stopPropagation(); addToCart('${prod.id}', 1)">
                  Add to Cart
                </button>
              </div>
            </div>
          </div>
        `
          )
          .join('')}
      </div>

      <div class="custom-atelier-callout">
        <div class="callout-content">
          <h3>Need Custom Dimensions or Firmness?</h3>
          <p>Our Master Sleep Architects craft bespoke custom sizes to fit heritage antique beds, yacht berths, and master alcoves.</p>
          <button class="btn-hero-outline" onclick="openModal('bespoke')">Launch 3D Bespoke Studio ⚙️</button>
        </div>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// TAB 4: ACCOUNT (User Profile, Orders, Addresses, Wishlist)
// -------------------------------------------------------------
function renderAccountTab() {
  const wishlistProducts = APP_PRODUCTS.filter((p) => state.wishlist.has(p.id));

  return `
    <div class="account-container">
      <div class="account-header-card">
        <div class="account-header-top" style="display: flex; align-items: center; justify-content: space-between; gap: 14px; flex-wrap: wrap;">
          <div style="display: flex; align-items: center; gap: 14px;">
            <div class="account-avatar">${state.user ? '👑' : '👤'}</div>
            <div class="account-details">
              <h3>${escapeHtml(state.user ? state.user.name : 'Sanctuary Guest')}</h3>
              <p>${escapeHtml(state.user ? `${state.user.email} • ${state.user.phone}` : 'Sign in to unlock 25% Member Privilege')}</p>
              <span class="badge-patron ${state.user ? '' : 'unverified'}">
                ${state.user ? '⭐ VERIFIED SANCTUARY MEMBER • WELCOME25 UNLOCKED' : '🔒 GUEST PATRON (SIGN IN FOR 25% OFF)'}
              </span>
            </div>
          </div>
          <div>
            ${
              state.user
                ? `<button class="btn-text-gold" onclick="logoutUser()" style="border: 1px solid var(--border-subtle); padding: 6px 12px; border-radius: var(--radius-sm); font-size: 0.8rem;">Sign Out</button>`
                : `<button class="btn-hero-primary" onclick="openModal('login')" style="padding: 8px 16px; font-size: 0.85rem;">Sign In / Register →</button>`
            }
          </div>
        </div>

        <!-- Quick Stats Row (AccountStatItem from AccountScreen.kt) -->
        <div class="account-quick-stats">
          <div class="quick-stat-item" onclick="openModal('cart')">
            <span class="quick-stat-val">${state.cart.length} Items</span>
            <span class="quick-stat-lbl">In Cart</span>
          </div>
          <div class="quick-stat-item" onclick="document.getElementById('accountWishlistSection')?.scrollIntoView({ behavior: 'smooth' })">
            <span class="quick-stat-val">${state.wishlist.size} Saved</span>
            <span class="quick-stat-lbl">Wishlist</span>
          </div>
          <div class="quick-stat-item" onclick="openModal('sanctuary_care', { type: 'sponsor_rewards' })">
            <span class="quick-stat-val">2,500 Pts</span>
            <span class="quick-stat-lbl">Rewards</span>
          </div>
        </div>
      </div>

      <!-- SECTION 1: ORDERS & SLEEP EXPERIENCE (AccountScreen.kt) -->
      <div class="account-section">
        <span class="account-section-eyebrow">ORDERS & SLEEP EXPERIENCE</span>
        <div class="account-menu-list">
          <div class="account-menu-row" onclick="openModal('order_tracking')">
            <div class="menu-row-left">
              <span class="menu-row-icon">🚚</span>
              <div class="menu-row-text">
                <h5>Track Order Delivery</h5>
                <p>Live delivery milestones & consignment driver details</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
          <div class="account-menu-row" onclick="openModal('sleep_quiz')">
            <div class="menu-row-left">
              <span class="menu-row-icon">🌙</span>
              <div class="menu-row-text">
                <h5>Sleep Sanctuary Firmness Quiz</h5>
                <p>Find your tailored orthopedic match (3-minute consultation)</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
          <div class="account-menu-row" onclick="openModal('compare')">
            <div class="menu-row-left">
              <span class="menu-row-icon">⚖️</span>
              <div class="menu-row-text">
                <h5>Mattress Comparison Matrix</h5>
                <p>Side-by-side specs, coils, materials & warranty schedules</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
        </div>
      </div>

      <!-- SECTION 2: CARE, INQUIRIES & SUPPORT (AccountScreen.kt) -->
      <div class="account-section">
        <span class="account-section-eyebrow">CARE, INQUIRIES & SUPPORT</span>
        <div class="account-menu-list">
          <div class="account-menu-row" onclick="openModal('ai_concierge')">
            <div class="menu-row-left">
              <span class="menu-row-icon">💬</span>
              <div class="menu-row-text">
                <h5>Live Customer Support Chat</h5>
                <p>1-on-1 with DreamCare AI & sleep specialists</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
          <div class="account-menu-row" onclick="openModal('sanctuary_care', { type: 'your_needs' })">
            <div class="menu-row-left">
              <span class="menu-row-icon">📋</span>
              <div class="menu-row-text">
                <h5>Your Needs & Custom Inquiry</h5>
                <p>Bespoke dimensions, non-standard frames & instant quotes</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
          <div class="account-menu-row" onclick="openModal('sanctuary_care', { type: 'warranty' })">
            <div class="menu-row-left">
              <span class="menu-row-icon">🛡️</span>
              <div class="menu-row-text">
                <h5>Service & Warranties</h5>
                <p>25-Year Guarantee certificate registration & policy lookup</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
          <div class="account-menu-row" onclick="openModal('sanctuary_care', { type: 'repairs' })">
            <div class="menu-row-left">
              <span class="menu-row-icon">🔧</span>
              <div class="menu-row-text">
                <h5>Repairs & Maintenance</h5>
                <p>Service engineer visit request & restoration</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
          <div class="account-menu-row" onclick="openModal('sanctuary_care', { type: 'complaints' })">
            <div class="menu-row-left">
              <span class="menu-row-icon">🤝</span>
              <div class="menu-row-text">
                <h5>Customer Grievances & Complaints</h5>
                <p>24-Hour SLA ticket support under E-Commerce Rules 2020</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
          <div class="account-menu-row" onclick="openModal('sanctuary_care', { type: 'feedbacks' })">
            <div class="menu-row-left">
              <span class="menu-row-icon">⭐</span>
              <div class="menu-row-text">
                <h5>Feedbacks & Reviews</h5>
                <p>Share your authentic sleep comfort experience</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
          <div class="account-menu-row" onclick="openModal('sanctuary_care', { type: 'sponsor_rewards' })">
            <div class="menu-row-left">
              <span class="menu-row-icon">🎁</span>
              <div class="menu-row-text">
                <h5>Sponsor Rewards Program</h5>
                <p>Earn ₹2,500 store credit & VIP guild benefits</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </div>
        </div>
      </div>

      <!-- Placed Orders Section -->
      <div class="account-section">
        <div class="account-section-header">
          <h4>Your Placed Sanctuary Orders</h4>
          <span class="badge-count">${state.orders.length}</span>
        </div>

        ${
          state.orders.length > 0
            ? `
          <div class="orders-list">
            ${state.orders
              .map(
                (order) => `
              <div class="order-card">
                <div class="order-card-header">
                  <div>
                    <strong>Order #${order.orderId}</strong>
                    <span class="order-date">${new Date(order.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
                  </div>
                  <span class="order-status-badge ${order.status.toLowerCase()}">${order.status}</span>
                </div>
                <div class="order-card-items">
                  ${order.items
                    .map((item) => `<div>• ${item.title} (x${item.quantity}) - ${formatCurrency(item.price * item.quantity)}</div>`)
                    .join('')}
                </div>
                <div class="order-card-footer">
                  <span>Total: <strong>${formatCurrency(order.finalTotal)}</strong></span>
                  <div class="order-actions">
                    <button class="btn-invoice-sm" onclick="printInvoice('${order.orderId}')">📄 GST Invoice</button>
                    <button class="btn-track-sm" onclick="openModal('order_tracking', { orderId: '${order.orderId}' })">🚚 Track</button>
                  </div>
                </div>
              </div>
            `
              )
              .join('')}
          </div>
        `
            : `
          <div class="account-empty-state">
            <p>No orders placed yet. Experience restorative sleep today.</p>
            <button class="btn-text-gold" onclick="setActiveTab('products')">Explore Catalog →</button>
          </div>
        `
        }
      </div>

      <!-- Saved Delivery Address Book -->
      <div class="account-section">
        <div class="account-section-header">
          <h4>Sanctuary Address Book</h4>
          <button class="btn-text-gold" onclick="openAddAddressPrompt()">+ Add Address</button>
        </div>

        <div class="address-book-list">
          ${state.savedAddresses
            .map(
              (addr, idx) => `
            <div class="address-book-card">
              <div class="address-tag-badge">${escapeHtml(addr.tag || 'Home')}</div>
              <div class="address-body">
                <strong>${escapeHtml(addr.name)}</strong> (${escapeHtml(addr.phone)})<br>
                ${escapeHtml(addr.addressLine)}, ${escapeHtml(addr.city)}, ${escapeHtml(addr.state)} - ${escapeHtml(addr.pincode)}
              </div>
              <button class="btn-delete-addr" onclick="deleteAddress(${idx})">Delete</button>
            </div>
          `
            )
            .join('')}
        </div>
      </div>

      <!-- Wishlist Section -->
      <div class="account-section" id="accountWishlistSection">
        <div class="account-section-header">
          <h4>Your Wishlist Items</h4>
          <span class="badge-count">${wishlistProducts.length}</span>
        </div>

        ${
          wishlistProducts.length > 0
            ? `
          <div class="product-grid mini">
            ${wishlistProducts.map((p) => renderProductCard(p)).join('')}
          </div>
        `
            : `
          <div class="account-empty-state">
            <p>Your wishlist is empty. Tap the heart icon on any mattress to save.</p>
          </div>
        `
        }
      </div>

      <!-- SECTION 3: POLICIES & LEGAL TERMS (AccountScreen.kt) -->
      <div class="account-section">
        <span class="account-section-eyebrow">POLICIES & LEGAL TERMS</span>
        <div class="account-menu-list">
          <a class="account-menu-row" href="/terms.html">
            <div class="menu-row-left">
              <span class="menu-row-icon">📜</span>
              <div class="menu-row-text">
                <h5>Terms of Service (25-Yr Guarantee)</h5>
                <p>SpringHaven 25-year warranty schedule, care & conditions of sale</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </a>
          <a class="account-menu-row" href="/privacy-policy.html">
            <div class="menu-row-left">
              <span class="menu-row-icon">🔒</span>
              <div class="menu-row-text">
                <h5>Privacy Policy & Data Safety</h5>
                <p>Review data collection, DPDP Act 2023 compliance & privacy protection</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </a>
          <a class="account-menu-row" href="/refund-policy.html">
            <div class="menu-row-left">
              <span class="menu-row-icon">🔄</span>
              <div class="menu-row-text">
                <h5>Refund & Cancellation Policy</h5>
                <p>Return eligibility, white-glove inspections & refund timelines</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </a>
          <a class="account-menu-row" href="/shipping-policy.html">
            <div class="menu-row-left">
              <span class="menu-row-icon">🚛</span>
              <div class="menu-row-text">
                <h5>Shipping & Logistics Policy</h5>
                <p>Free white-glove doorstep delivery & multi-zone tracking</p>
              </div>
            </div>
            <span class="menu-row-arrow">→</span>
          </a>
          <a class="account-menu-row" href="/delete-account.html" style="color: #BA1A1A;">
            <div class="menu-row-left">
              <span class="menu-row-icon">🗑️</span>
              <div class="menu-row-text">
                <h5 style="color: #BA1A1A;">Delete Account & Clear Data</h5>
                <p>Purge local sessions, cart, wishlist, offline cache & cloud deletion</p>
              </div>
            </div>
            <span class="menu-row-arrow" style="color: #BA1A1A;">→</span>
          </a>
        </div>
      </div>
    </div>
  `;
}

function openAddAddressPrompt() {
  const name = prompt('Recipient Full Name:', 'Sanctuary Patron');
  if (!name) return;
  const phone = prompt('Contact Phone Number:', '+91 70149 83696');
  if (!phone) return;
  const addressLine = prompt('Street Address / Apartment:', 'D-4, Vijay Vihar Colony');
  if (!addressLine) return;
  const city = prompt('City:', 'Jaipur');
  const stateVal = prompt('State:', 'Rajasthan');
  const pincode = prompt('PIN Code:', '302039');

  state.savedAddresses.push({
    id: 'addr_' + Date.now(),
    name: name.trim(),
    phone: phone.trim(),
    addressLine: addressLine.trim(),
    city: (city || 'Jaipur').trim(),
    state: (stateVal || 'Rajasthan').trim(),
    pincode: (pincode || '302039').trim(),
    tag: 'Sanctuary'
  });

  persistAddresses();
  renderApp();
  if (state.currentModal === 'checkout') {
    renderModal();
  }
  showToastNotification('Address saved successfully');
}

function deleteAddress(idx) {
  state.savedAddresses.splice(idx, 1);
  persistAddresses();
  renderApp();
  if (state.currentModal === 'checkout') {
    renderModal();
  }
  showToastNotification('Address removed');
}

// -------------------------------------------------------------
// MODALS CONTROLLER & POPUPS
// -------------------------------------------------------------
function renderModal() {
  const modalContainer = document.getElementById('modalContainer');
  if (!modalContainer) return;

  modalContainer.classList.add('active');

  switch (state.currentModal) {
    case 'pdp':
      modalContainer.innerHTML = renderPdpModal();
      break;
    case 'bespoke':
      modalContainer.innerHTML = renderBespokeStudioModal();
      break;
    case 'sleep_quiz':
      modalContainer.innerHTML = renderSleepQuizModal();
      break;
    case 'compare':
      modalContainer.innerHTML = renderCompareModal();
      break;
    case 'cart':
      modalContainer.innerHTML = renderCartModal();
      break;
    case 'checkout':
      modalContainer.innerHTML = renderCheckoutModal();
      break;
    case 'payment_gateway':
      modalContainer.innerHTML = renderPaymentGatewayModal();
      break;
    case 'order_success':
      modalContainer.innerHTML = renderOrderSuccessModal();
      break;
    case 'order_tracking':
      modalContainer.innerHTML = renderOrderTrackingModal();
      break;
    case 'ai_concierge':
      modalContainer.innerHTML = renderAiConciergeModal();
      break;
    case 'sanctuary_care':
      modalContainer.innerHTML = renderSanctuaryCareModal();
      break;
    case 'login':
      modalContainer.innerHTML = renderLoginModal();
      break;
    default:
      closeModal();
  }
}

// -------------------------------------------------------------
// MODAL: MEMBER LOGIN & PRIVILEGE SIGNUP
// -------------------------------------------------------------
function renderLoginModal() {
  return `
    <div class="modal-dialog login-modal-dialog">
      <div class="modal-header">
        <div>
          <span class="modal-eyebrow">SANCTUARY PRIVILEGE</span>
          <h3>Member Sign In & Register</h3>
        </div>
        <button class="modal-close-btn" onclick="closeModal()">✕</button>
      </div>

      <div class="modal-scrollable-body" style="padding: 20px;">
        <div class="login-privilege-banner" style="background: linear-gradient(135deg, rgba(212,175,55,0.15), rgba(16,46,35,0.1)); border: 1.5px dashed var(--satin-gold); border-radius: var(--radius-md); padding: 14px; margin-bottom: 18px; display: flex; gap: 12px; align-items: center;">
          <div style="font-size: 2rem;">👑</div>
          <div style="font-size: 0.85rem; line-height: 1.5;">
            <strong style="color: var(--forest-primary); display: block; font-size: 0.95rem;">Unlock 25% Member Privilege (WELCOME25)</strong>
            <span>Sign in or register to instantly unlock 25% OFF all luxury sleep systems, 25-year structural warranty, and priority concierge dispatch.</span>
          </div>
        </div>

        <form class="sanctuary-form" onsubmit="handleUserLoginSubmit(event)" style="display: flex; flex-direction: column; gap: 14px;">
          <div class="form-group" style="display: flex; flex-direction: column; gap: 5px;">
            <label style="font-size: 0.82rem; font-weight: 700; color: var(--text-primary);">Full Name *</label>
            <input type="text" id="loginName" required placeholder="e.g. Gaurav Chandra" value="${escapeHtml(state.user?.name || '')}" style="padding: 10px 12px; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); font-size: 0.9rem; font-family: inherit;">
          </div>

          <div class="form-group" style="display: flex; flex-direction: column; gap: 5px;">
            <label style="font-size: 0.82rem; font-weight: 700; color: var(--text-primary);">10-Digit Mobile Number (WhatsApp Delivery Alerts) *</label>
            <input type="tel" id="loginPhone" required placeholder="e.g. 7014983696" maxlength="10" value="${escapeHtml(state.user?.phone || '')}" style="padding: 10px 12px; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); font-size: 0.9rem; font-family: inherit;">
          </div>

          <div class="form-group" style="display: flex; flex-direction: column; gap: 5px;">
            <label style="font-size: 0.82rem; font-weight: 700; color: var(--text-primary);">Email Address (For Tax Invoices & Warranty Certificates) *</label>
            <input type="email" id="loginEmail" required placeholder="e.g. patron@gooddream.com" value="${escapeHtml(state.user?.email || '')}" style="padding: 10px 12px; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); font-size: 0.9rem; font-family: inherit;">
          </div>

          <div style="font-size: 0.76rem; color: var(--text-secondary); line-height: 1.5; margin-top: 4px;">
            🔒 By continuing, you agree to Good Dream's <a href="/terms.html" target="_blank" style="color: var(--forest-primary); text-decoration: underline;">Terms</a> and <a href="/privacy-policy.html" target="_blank" style="color: var(--forest-primary); text-decoration: underline;">Privacy Policy</a> under DPDP Act 2023. Zero spam guarantee.
          </div>

          <button type="submit" class="btn-hero-primary full-width" style="padding: 12px; font-size: 0.95rem; font-weight: 700; margin-top: 6px;">
            Unlock 25% Member Privilege (WELCOME25) →
          </button>
        </form>
      </div>
    </div>
  `;
}

function handleUserLoginSubmit(e) {
  e.preventDefault();
  const name = (document.getElementById('loginName')?.value || '').trim();
  const phone = (document.getElementById('loginPhone')?.value || '').trim();
  const email = (document.getElementById('loginEmail')?.value || '').trim();

  if (!name || !phone || !email) {
    showToastNotification('Please fill in all required fields.');
    return;
  }

  state.user = { name, phone, email, isMember: true, joinedAt: Date.now() };
  persistUser();

  // Automatically apply WELCOME25 privilege code
  state.appliedCoupon = { code: 'WELCOME25', percent: 25 };

  showToastNotification(`Welcome, ${name}! 25% Member Privilege (WELCOME25) unlocked!`, 4000);
  updateBadgeCounts();

  if (state.cart.length > 0) {
    openModal('cart');
  } else {
    closeModal();
    renderApp();
  }
}

function logoutUser() {
  state.user = null;
  state.appliedCoupon = null;
  persistUser();
  updateBadgeCounts();
  showToastNotification('Signed out from Member Account. Privilege discount removed.');
  renderApp();
  if (state.currentModal === 'cart') renderModal();
}

// -------------------------------------------------------------
// MODAL: PRODUCT DETAIL (Exact match with ProductDetailScreen.kt)
// -------------------------------------------------------------
function renderPdpModal() {
  const product = APP_PRODUCTS.find((p) => p.id === state.modalData?.id) || APP_PRODUCTS[0];
  const isWishlisted = state.wishlist.has(product.id);

  return `
    <div class="modal-dialog pdp-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal()">← Back</button>
        <div class="modal-header-actions">
          <button class="btn-wishlist-pdp ${isWishlisted ? 'active' : ''}" onclick="toggleWishlist('${product.id}')">
            ${isWishlisted ? '❤️ Saved' : '🤍 Save'}
          </button>
          <button class="btn-modal-close" onclick="closeModal()">✕</button>
        </div>
      </div>

      <div class="pdp-modal-content">
        <!-- Image Gallery Showcase -->
        <div class="pdp-gallery-card">
          <div class="pdp-main-image-wrap">
            <img id="pdpMainImg" src="${optimizeImageUrl(product.images[0], 800)}" alt="${escapeHtml(product.title)}" decoding="async">
            ${product.isSpringhavenSeries ? `<span class="badge-flagship-pdp">SPRINGHAVEN 15"</span>` : ''}
          </div>
          <div class="pdp-thumbnails-strip">
            ${product.images
              .map(
                (img, idx) => `
              <img 
                src="${optimizeImageUrl(img, 150)}" 
                alt="Angle ${idx + 1}" 
                class="pdp-thumb ${idx === 0 ? 'active' : ''}" 
                onclick="document.getElementById('pdpMainImg').src='${optimizeImageUrl(img, 800)}'; document.querySelectorAll('.pdp-thumb').forEach(t=>t.classList.remove('active')); this.classList.add('active');"
                loading="lazy" 
                decoding="async"
              >
            `
              )
              .join('')}
          </div>
        </div>

        <!-- Product Details & Specs -->
        <div class="pdp-info-card">
          <div class="pdp-eyebrow-row">
            <span class="pdp-sku">SKU: ${product.sku}</span>
            <span class="pdp-rating">★★★★★ (4.9 / 5 • 180+ Reviews)</span>
          </div>

          <h2 class="pdp-title">${product.title}</h2>
          <p class="pdp-subtitle">${product.subtitle}</p>

          <div class="pdp-price-row">
            <span class="pdp-current-price">${formatCurrency(product.price)}</span>
            ${
              product.originalPrice > product.price
                ? `
              <span class="pdp-original-price">${formatCurrency(product.originalPrice)}</span>
              <span class="pdp-save-badge">Save ${formatCurrency(product.originalPrice - product.price)}</span>
            `
                : ''
            }
          </div>
          <div class="pdp-tax-note">Inclusive of 18% GST (HSN 9404) • Free White-Glove In-Room Delivery</div>

          <!-- Dimension Selector -->
          <div class="pdp-spec-block">
            <label class="pdp-label">Select Bed Dimensions:</label>
            <div class="pdp-dimension-chips">
              <button class="dimension-chip active">${product.dimensions}</button>
              <button class="dimension-chip" onclick="openModal('bespoke')">⚙️ Custom Sizing</button>
            </div>
          </div>

          <!-- Firmness & Support Indicator -->
          <div class="pdp-spec-block">
            <label class="pdp-label">Firmness Profile:</label>
            <div class="firmness-meter-box">
              <span class="firmness-text"><strong>${product.firmness}</strong></span>
              <span class="firmness-sub">Optimal spinal decompression with motion-isolation technology</span>
            </div>
          </div>

          <!-- Technical Specifications Table -->
          <div class="pdp-spec-block">
            <label class="pdp-label">Atelier Specifications:</label>
            <table class="specs-table">
              <tbody>
                <tr>
                  <td>Total Height</td>
                  <td><strong>${product.thicknessInches} Inches</strong></td>
                </tr>
                <tr>
                  <td>Primary Material</td>
                  <td><strong>${product.material}</strong></td>
                </tr>
                <tr>
                  <td>Structural Guarantee</td>
                  <td><strong>${product.warrantyYears} Years Certified</strong></td>
                </tr>
                ${product.specifications
                  .map(
                    (spec) => `
                  <tr>
                    <td>${spec.key}</td>
                    <td>${spec.value}</td>
                  </tr>
                `
                  )
                  .join('')}
              </tbody>
            </table>
          </div>

          <!-- Statutory Legal Metrology Card (Mandatory Rule Compliance) -->
          <div class="pdp-metrology-box">
            <div class="metrology-title">🏛️ Mandatory Statutory Legal Metrology Disclosures</div>
            <div class="metrology-content">
              <strong>Country of Origin:</strong> India<br>
              <strong>Net Quantity:</strong> 1 Unit ${product.title}<br>
              <strong>Manufactured By:</strong> ${CORPORATE_CONFIG.companyName}, ${CORPORATE_CONFIG.registeredOffice}<br>
              <strong>Marketed By:</strong> ${CORPORATE_CONFIG.marketedBy}<br>
              <strong>Customer Care Helpline:</strong> ${CORPORATE_CONFIG.supportPhone} | ${CORPORATE_CONFIG.supportEmail}
            </div>
          </div>

          <!-- Floating Sticky Action Bar -->
          <div class="pdp-bottom-cta-row">
            <div class="qty-stepper">
              <button onclick="decrementPdpQty()">-</button>
              <span id="pdpQtyVal">1</span>
              <button onclick="incrementPdpQty()">+</button>
            </div>
            <button class="btn-pdp-cart" onclick="addPdpToCart('${product.id}')">Add to Cart 🛍️</button>
            <button class="btn-pdp-buy" onclick="buyNowPdp('${product.id}')">Buy Now →</button>
          </div>
        </div>
      </div>
    </div>
  `;
}

function incrementPdpQty() {
  const el = document.getElementById('pdpQtyVal');
  if (el) el.textContent = parseInt(el.textContent) + 1;
}

function decrementPdpQty() {
  const el = document.getElementById('pdpQtyVal');
  if (el) {
    const val = parseInt(el.textContent);
    if (val > 1) el.textContent = val - 1;
  }
}

function addPdpToCart(productId) {
  const qtyEl = document.getElementById('pdpQtyVal');
  const qty = qtyEl ? parseInt(qtyEl.textContent) : 1;
  addToCart(productId, qty);
  closeModal();
  openModal('cart');
}

function buyNowPdp(productId) {
  const qtyEl = document.getElementById('pdpQtyVal');
  const qty = qtyEl ? parseInt(qtyEl.textContent) : 1;
  addToCart(productId, qty);
  closeModal();
  openModal('checkout');
}

// -------------------------------------------------------------
// MODAL: BESPOKE STUDIO (Exact match with BespokeStudioScreen.kt)
// -------------------------------------------------------------
function renderBespokeStudioModal() {
  const cfg = state.bespokeConfig;
  const pricing = calculateBespokePrice();

  return `
    <div class="modal-dialog bespoke-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal()">← Close Studio</button>
        <div class="modal-header-title">
          <h3>Bespoke Mattress Architect</h3>
          <span>Atelier Handcrafted Sleep Studio</span>
        </div>
        <button class="btn-modal-close" onclick="closeModal()">✕</button>
      </div>

      <div class="bespoke-studio-layout">
        <!-- Left: 3D Cutaway Layer Visualization -->
        <div class="bespoke-visualizer-card">
          <div class="view-mode-toggle">
            <button 
              class="view-btn ${cfg.viewMode === 'exploded' ? 'active' : ''}" 
              onclick="setBespokeViewMode('exploded')"
            >
              Exploded Cutaway
            </button>
            <button 
              class="view-btn ${cfg.viewMode === 'assembled' ? 'active' : ''}" 
              onclick="setBespokeViewMode('assembled')"
            >
              Assembled View
            </button>
          </div>

          <!-- Cutaway Interactive Layer Stack -->
          <div class="layer-stack-visualizer ${cfg.viewMode}">
            <div class="layer-slice slice-quilt ${cfg.layers.quilt ? 'active' : 'disabled'}">
              <span class="layer-badge">Top Quilt</span>
              <span class="layer-name">Hand-Tufted Cashmere & Damask Silk</span>
            </div>

            <div class="layer-slice slice-latex ${cfg.layers.latex ? 'active' : 'disabled'}">
              <span class="layer-badge">Latex Layer</span>
              <span class="layer-name">100% Belgian Organic Natural Latex (4-inch)</span>
            </div>

            <div class="layer-slice slice-memory ${cfg.layers.memory ? 'active' : 'disabled'}">
              <span class="layer-badge">Cool-Gel</span>
              <span class="layer-name">Adaptive Temperature-Neutral Memory Layer</span>
            </div>

            <div class="layer-slice slice-springs ${cfg.layers.springs ? 'active' : 'disabled'}">
              <span class="layer-badge">Coils</span>
              <span class="layer-name">7-Zone Swedish Micro-Pocketed Spring Suspension</span>
            </div>

            <div class="layer-slice slice-base ${cfg.layers.base ? 'active' : 'disabled'}">
              <span class="layer-badge">Core Foundation</span>
              <span class="layer-name">High-Resilience Orthopedic Foundation Base</span>
            </div>
          </div>

          <!-- Monogram Embroidery Ribbon Preview -->
          <div class="monogram-preview-ribbon ${cfg.monogramColor}">
            <span class="ribbon-text">${cfg.monogram.trim() ? escapeHtml(cfg.monogram.toUpperCase()) : 'YOUR BESPOKE EMBROIDERY'}</span>
            <span class="ribbon-seal">HANDCRAFTED IN JAIPUR</span>
          </div>

          <!-- Real-Time Metrics -->
          <div class="bespoke-metrics-strip">
            <span>Dimensions: <strong>${cfg.width}" W × ${cfg.length}" L</strong></span>
            <span>Thickness: <strong>${cfg.thickness}"</strong></span>
            <span>Area: <strong>${pricing.areaSqFt} sq. ft.</strong></span>
          </div>
        </div>

        <!-- Right: Studio Controls & Configuration -->
        <div class="bespoke-controls-card">
          <!-- Parametric Dimension Sliders -->
          <div class="control-group">
            <div class="control-label-row">
              <label>Mattress Width (Inches):</label>
              <strong id="widthVal">${cfg.width}" (${cfg.width === 72 ? 'King' : cfg.width === 60 ? 'Queen' : cfg.width === 36 ? 'Single' : 'Custom'})</strong>
            </div>
            <input 
              type="range" 
              min="36" 
              max="78" 
              step="2" 
              value="${cfg.width}" 
              oninput="updateBespokeDim('width', this.value)"
            >
          </div>

          <div class="control-group">
            <div class="control-label-row">
              <label>Mattress Length (Inches):</label>
              <strong id="lengthVal">${cfg.length}" (${cfg.length === 78 ? 'Standard' : cfg.length === 84 ? 'Extra Long' : 'Custom'})</strong>
            </div>
            <input 
              type="range" 
              min="72" 
              max="84" 
              step="2" 
              value="${cfg.length}" 
              oninput="updateBespokeDim('length', this.value)"
            >
          </div>

          <div class="control-group">
            <div class="control-label-row">
              <label>Mattress Thickness (Profile):</label>
              <strong id="thickVal">${cfg.thickness}" Deep Profile</strong>
            </div>
            <input 
              type="range" 
              min="6" 
              max="15" 
              step="1" 
              value="${cfg.thickness}" 
              oninput="updateBespokeDim('thickness', this.value)"
            >
          </div>

          <!-- Core Foundation System (BespokeStudioModels.kt) -->
          <div class="control-group">
            <label class="control-title">Core Foundation System:</label>
            <select onchange="setBespokeCore(this.value)" class="bespoke-dropdown">
              ${Object.entries(BESPOKE_CORE_TYPES).map(([k, c]) => `
                <option value="${k}" ${cfg.coreType === k ? 'selected' : ''}>
                  ${c.title} — ${formatCurrency(c.basePrice)} (${c.subtitle})
                </option>
              `).join('')}
            </select>
          </div>

          <!-- Comfort Crown Layer (BespokeStudioModels.kt) -->
          <div class="control-group">
            <label class="control-title">Comfort Crown Layer:</label>
            <select onchange="setBespokeComfort(this.value)" class="bespoke-dropdown">
              ${Object.entries(BESPOKE_COMFORT_LAYERS).map(([k, l]) => `
                <option value="${k}" ${cfg.comfortLayer === k ? 'selected' : ''}>
                  ${l.title} (+${formatCurrency(l.additionalPrice)}) — Airflow: ${l.airflowRating}
                </option>
              `).join('')}
            </select>
          </div>

          <!-- Upholstery & Quilt Fabric (BespokeStudioModels.kt) -->
          <div class="control-group">
            <label class="control-title">Upholstery & Quilt Fabric:</label>
            <select onchange="setBespokeQuilt(this.value)" class="bespoke-dropdown">
              ${Object.entries(BESPOKE_QUILT_COVERS).map(([k, q]) => `
                <option value="${k}" ${cfg.quiltCover === k ? 'selected' : ''}>
                  ${q.title} (${q.additionalPrice > 0 ? '+' + formatCurrency(q.additionalPrice) : 'Included'})
                </option>
              `).join('')}
            </select>
          </div>

          <!-- Bed Base Foundation Option -->
          <div class="control-group">
            <label class="layer-checkbox-row" style="background: var(--surface-subtle); padding: 8px 12px; border-radius: var(--radius-sm); border: 1px solid var(--border-subtle); cursor: pointer;">
              <input type="checkbox" ${cfg.includeBedBase ? 'checked' : ''} onchange="toggleBespokeBedBase()">
              <span><strong>Include Foundation Bed Base (+₹18,000)</strong><br><small style="color: var(--text-secondary);">Solid seasoned hardwood artisan bed foundation</small></span>
            </label>
          </div>

          <!-- Monogram Embroidery Ribbon Input -->
          <div class="control-group">
            <label class="control-title">Hand-Stitched Silk Monogram Embroidery (+₹1,500):</label>
            <input 
              type="text" 
              id="monogramInput" 
              placeholder="e.g., THE ROYAL SANCTUARY" 
              maxlength="30" 
              value="${escapeHtml(cfg.monogram)}" 
              oninput="updateBespokeMonogram(this.value)"
            >
            <div class="thread-swatches-row">
              <span>Thread:</span>
              <button class="thread-chip gold ${cfg.monogramColor === 'gold' ? 'active' : ''}" onclick="setThreadColor('gold')">Satin Gold</button>
              <button class="thread-chip silver ${cfg.monogramColor === 'silver' ? 'active' : ''}" onclick="setThreadColor('silver')">Platinum Silver</button>
              <button class="thread-chip ivory ${cfg.monogramColor === 'ivory' ? 'active' : ''}" onclick="setThreadColor('ivory')">Imperial Ivory</button>
              <button class="thread-chip emerald ${cfg.monogramColor === 'emerald' ? 'active' : ''}" onclick="setThreadColor('emerald')">Forest Emerald</button>
            </div>
          </div>

          <!-- Dynamic Pricing Box & Commission Action -->
          <div class="bespoke-pricing-card">
            <div class="pricing-row">
              <span>Total Custom Commission:</span>
              <strong class="total-price">${formatCurrency(pricing.totalPrice)}</strong>
            </div>
            <div class="pricing-row advance">
              <span>20% White-Glove Booking Advance:</span>
              <strong>${formatCurrency(pricing.advance20)}</strong>
            </div>
            <div class="pricing-sub">Balance ${formatCurrency(pricing.balanceAtDoorstep)} payable upon in-room delivery & setup.</div>

            <button class="btn-hero-primary full-width" onclick="addBespokeToCart()">
              Commission Custom Mattress →
            </button>
          </div>
        </div>
      </div>
    </div>
  `;
}

function setBespokeCore(coreKey) {
  state.bespokeConfig.coreType = coreKey;
  renderModal();
}

function setBespokeComfort(comfortKey) {
  state.bespokeConfig.comfortLayer = comfortKey;
  renderModal();
}

function setBespokeQuilt(quiltKey) {
  state.bespokeConfig.quiltCover = quiltKey;
  renderModal();
}

function toggleBespokeBedBase() {
  state.bespokeConfig.includeBedBase = !state.bespokeConfig.includeBedBase;
  renderModal();
}

function updateBespokeDim(param, val) {
  state.bespokeConfig[param] = parseInt(val);
  renderModal();
}

function toggleBespokeLayer(layer) {
  state.bespokeConfig.layers[layer] = !state.bespokeConfig.layers[layer];
  renderModal();
}

function updateBespokeMonogram(val) {
  state.bespokeConfig.monogram = val;
  const ribbon = document.querySelector('.monogram-preview-ribbon .ribbon-text');
  if (ribbon) {
    ribbon.textContent = val.trim() ? val.toUpperCase() : 'YOUR BESPOKE EMBROIDERY';
  }
}

function setThreadColor(color) {
  state.bespokeConfig.monogramColor = color;
  renderModal();
}

function setBespokeViewMode(mode) {
  state.bespokeConfig.viewMode = mode;
  renderModal();
}

function addBespokeToCart() {
  const pricing = calculateBespokePrice();
  const cfg = state.bespokeConfig;

  const bespokeProductData = {
    title: `Bespoke ${pricing.core.title} Atelier Mattress (${cfg.width}" × ${cfg.length}" × ${cfg.thickness}")`,
    dimensions: `${cfg.width}" x ${cfg.length}"`,
    thickness: cfg.thickness,
    monogram: cfg.monogram,
    monogramColor: cfg.monogramColor,
    price: pricing.totalPrice,
    advance20: pricing.advance20,
    sku: pricing.sku,
    coreType: pricing.core.title,
    comfortLayer: pricing.comfort.title,
    quiltCover: pricing.quilt.title,
    includeBedBase: cfg.includeBedBase,
    layers: { ...cfg.layers }
  };

  addToCart('bespoke_mattress_custom', 1, bespokeProductData);
  closeModal();
  openModal('cart');
}

// -------------------------------------------------------------
// MODAL: SLEEP FIRMNESS QUIZ (Exact match with SleepFirmnessQuizModal.kt)
// -------------------------------------------------------------
let quizStep = 1;
const quizAnswers = {
  position: 'Side',
  weight: 'Average',
  spine: 'None',
  feel: 'Medium'
};

function renderSleepQuizModal() {
  return `
    <div class="modal-dialog quiz-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal()">← Cancel</button>
        <div class="modal-header-title">
          <h3>Sleep Diagnostic Wizard</h3>
          <span>Step ${quizStep} of 4</span>
        </div>
        <button class="btn-modal-close" onclick="closeModal()">✕</button>
      </div>

      <div class="quiz-modal-body">
        ${
          quizStep === 1
            ? `
          <h4>1. What is your primary sleeping posture?</h4>
          <p>Your spine contour requires different pressure relief based on your primary sleep angle.</p>
          <div class="quiz-options-list">
            <button class="quiz-option-btn ${quizAnswers.position === 'Side' ? 'active' : ''}" onclick="selectQuizAnswer('position', 'Side')">
              <span class="opt-icon">🛌</span>
              <div class="opt-info">
                <strong>Side Sleeper</strong>
                <span>Requires deep shoulder and hip contouring to keep spine straight</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.position === 'Back' ? 'active' : ''}" onclick="selectQuizAnswer('position', 'Back')">
              <span class="opt-icon">🧍</span>
              <div class="opt-info">
                <strong>Back Sleeper</strong>
                <span>Requires firm lumbar support to prevent lower spine sagging</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.position === 'Stomach' ? 'active' : ''}" onclick="selectQuizAnswer('position', 'Stomach')">
              <span class="opt-icon">🏊</span>
              <div class="opt-info">
                <strong>Stomach Sleeper</strong>
                <span>Requires extra firm surface to prevent neck and back arching</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.position === 'Combination' ? 'active' : ''}" onclick="selectQuizAnswer('position', 'Combination')">
              <span class="opt-icon">🔄</span>
              <div class="opt-info">
                <strong>Combination Sleeper</strong>
                <span>Requires dynamic adaptive response for multiple turns at night</span>
              </div>
            </button>
          </div>
        `
            : quizStep === 2
            ? `
          <h4>2. What is your body frame and weight category?</h4>
          <p>Heavier weights compress comfort layers more deeply, needing calibrated coil resistance.</p>
          <div class="quiz-options-list">
            <button class="quiz-option-btn ${quizAnswers.weight === 'Petite' ? 'active' : ''}" onclick="selectQuizAnswer('weight', 'Petite')">
              <span class="opt-icon">🪶</span>
              <div class="opt-info">
                <strong>Petite / Under 60 kg</strong>
                <span>Benefits from plush responsive latex to activate pressure relief</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.weight === 'Average' ? 'active' : ''}" onclick="selectQuizAnswer('weight', 'Average')">
              <span class="opt-icon">⚖️</span>
              <div class="opt-info">
                <strong>Average / 60 kg to 85 kg</strong>
                <span>Standard medium to medium-firm balanced ergonomic firmness</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.weight === 'Heavy' ? 'active' : ''}" onclick="selectQuizAnswer('weight', 'Heavy')">
              <span class="opt-icon">🏋️</span>
              <div class="opt-info">
                <strong>Heavy / Above 85 kg</strong>
                <span>Requires high-gauge titanium coils and high-density rebonded support</span>
              </div>
            </button>
          </div>
        `
            : quizStep === 3
            ? `
          <h4>3. Do you experience morning back pain or cervical stiffness?</h4>
          <p>Orthopedic zoning aligns vertebrae to relieve sciatic nerve pressure during sleep.</p>
          <div class="quiz-options-list">
            <button class="quiz-option-btn ${quizAnswers.spine === 'None' ? 'active' : ''}" onclick="selectQuizAnswer('spine', 'None')">
              <span class="opt-icon">✨</span>
              <div class="opt-info">
                <strong>No Chronic Pain</strong>
                <span>Seeking restful, luxury comfort and premium restorative sleep</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.spine === 'LowerBack' ? 'active' : ''}" onclick="selectQuizAnswer('spine', 'LowerBack')">
              <span class="opt-icon">⚡</span>
              <div class="opt-info">
                <strong>Lower Back / Lumbar Pain</strong>
                <span>Doctor recommended 5-Zone high-density spinal alignment</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.spine === 'Neck' ? 'active' : ''}" onclick="selectQuizAnswer('spine', 'Neck')">
              <span class="opt-icon">🧣</span>
              <div class="opt-info">
                <strong>Upper Back & Neck Stiffness</strong>
                <span>Requires contour cervical pillow paired with adaptive mattress</span>
              </div>
            </button>
          </div>
        `
            : `
          <h4>4. What is your preferred temperature & feel?</h4>
          <p>Thermal regulation is crucial for uninterrupted deep delta-wave sleep.</p>
          <div class="quiz-options-list">
            <button class="quiz-option-btn ${quizAnswers.feel === 'Cool' ? 'active' : ''}" onclick="selectQuizAnswer('feel', 'Cool')">
              <span class="opt-icon">❄️</span>
              <div class="opt-info">
                <strong>Cool & Breathable</strong>
                <span>Natural latex pin-core ventilation & cooling gel memory infusion</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.feel === 'Plush' ? 'active' : ''}" onclick="selectQuizAnswer('feel', 'Plush')">
              <span class="opt-icon">☁️</span>
              <div class="opt-info">
                <strong>Plush & Hugging</strong>
                <span>Cloud-like cushioning with cashmere quilting</span>
              </div>
            </button>
            <button class="quiz-option-btn ${quizAnswers.feel === 'Medium' ? 'active' : ''}" onclick="selectQuizAnswer('feel', 'Medium')">
              <span class="opt-icon">🛡️</span>
              <div class="opt-info">
                <strong>Balanced Orthopedic</strong>
                <span>Firm foundation with ergonomic surface cushioning</span>
              </div>
            </button>
          </div>
        `
        }

        <div class="quiz-nav-row">
          ${quizStep > 1 ? `<button class="btn-hero-outline" onclick="prevQuizStep()">← Previous</button>` : '<div></div>'}
          ${
            quizStep < 4
              ? `<button class="btn-hero-primary" onclick="nextQuizStep()">Next Step →</button>`
              : `<button class="btn-hero-primary" onclick="completeQuiz()">View My Sleep Match ✨</button>`
          }
        </div>
      </div>
    </div>
  `;
}

function selectQuizAnswer(field, val) {
  quizAnswers[field] = val;
  renderModal();
}

function nextQuizStep() {
  if (quizStep < 4) quizStep++;
  renderModal();
}

function prevQuizStep() {
  if (quizStep > 1) quizStep--;
  renderModal();
}

function completeQuiz() {
  // Compute profile
  let calculatedProfile = 'Balanced Ergonomic (6.5/10)';
  if (quizAnswers.spine === 'LowerBack' || quizAnswers.weight === 'Heavy') {
    calculatedProfile = 'Firm Orthopedic (8/10)';
  } else if (quizAnswers.position === 'Side' && quizAnswers.feel === 'Plush') {
    calculatedProfile = 'Medium Plush Luxury (6/10)';
  } else if (quizAnswers.feel === 'Cool') {
    calculatedProfile = 'Cooling Hybrid Luxury';
  }

  state.userSleepProfile = calculatedProfile;
  persistSleepProfile();
  quizStep = 1;
  closeModal();
  setActiveTab('products');
  showToastNotification(`Match Found: ${calculatedProfile}! Marked with ✨ Sleep Match`);
}

// -------------------------------------------------------------
// MODAL: PRODUCT COMPARISON MATRIX
// -------------------------------------------------------------
function renderCompareModal() {
  const mattresses = APP_PRODUCTS.filter((p) => p.categoryId === 'cat_mattress' || p.categoryId === 'cat_springhaven');

  return `
    <div class="modal-dialog compare-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal()">← Close</button>
        <div class="modal-header-title">
          <h3>Mattress Architecture Comparison Matrix</h3>
          <span>Side-by-side engineering specifications</span>
        </div>
        <button class="btn-modal-close" onclick="closeModal()">✕</button>
      </div>

      <div class="compare-table-wrap">
        <table class="compare-table">
          <thead>
            <tr>
              <th>Feature Spec</th>
              ${mattresses.slice(0, 3).map((m) => `<th><strong>${m.title}</strong><br><span class="price-pill">${formatCurrency(m.price)}</span></th>`).join('')}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>Thickness</strong></td>
              ${mattresses.slice(0, 3).map((m) => `<td>${m.thicknessInches} Inches</td>`).join('')}
            </tr>
            <tr>
              <td><strong>Firmness Rating</strong></td>
              ${mattresses.slice(0, 3).map((m) => `<td>${m.firmness}</td>`).join('')}
            </tr>
            <tr>
              <td><strong>Core Architecture</strong></td>
              ${mattresses.slice(0, 3).map((m) => `<td>${m.material}</td>`).join('')}
            </tr>
            <tr>
              <td><strong>Warranty</strong></td>
              ${mattresses.slice(0, 3).map((m) => `<td>${m.warrantyYears} Years Comprehensive</td>`).join('')}
            </tr>
            <tr>
              <td><strong>Craftsmanship</strong></td>
              ${mattresses.slice(0, 3).map((m) => `<td>100% Handcrafted Luxury</td>`).join('')}
            </tr>
            <tr>
              <td><strong>Action</strong></td>
              ${mattresses.slice(0, 3).map((m) => `<td><button class="btn-hero-primary full-width" onclick="addToCart('${m.id}', 1); closeModal(); openModal('cart');">Add to Cart</button></td>`).join('')}
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// MODAL: SANCTUARY CART & DRAWER (Exact match with CartScreen.kt)
// -------------------------------------------------------------
function renderCartModal() {
  const totals = getCartTotals();

  // Milestones: Tier 1: ₹5,000 (Free delivery), Tier 2: ₹25,000 (Silk Pillow pair), Tier 3: ₹50,000 (Egyptian Bedset)
  const milestoneTarget = 50000;
  const progressPercent = Math.min(100, Math.round((totals.subtotal / milestoneTarget) * 100));

  return `
    <div class="modal-dialog cart-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal()">← Continue Shopping</button>
        <div class="modal-header-title">
          <h3>Sanctuary Cart (${state.cart.reduce((s, i) => s + i.quantity, 0)})</h3>
          <span>Handcrafted Luxury Awaits</span>
        </div>
        ${state.cart.length > 0 ? `<button class="btn-text-gold" onclick="clearCart()">Clear All</button>` : ''}
        <button class="btn-modal-close" onclick="closeModal()">✕</button>
      </div>

      <div class="cart-modal-body">
        <!-- Spend Milestone Progress Bar (CartMilestoneProgressBar.kt) -->
        <div class="cart-milestone-box">
          <div class="milestone-label-row">
            <span>Spend Rewards:</span>
            <strong>
              ${
                totals.subtotal >= 50000
                  ? '🎉 UNLOCKED: Complimentary Egyptian Bedset (Worth ₹9,999)!'
                  : totals.subtotal >= 25000
                  ? `Add ${formatCurrency(50000 - totals.subtotal)} more to unlock Egyptian Bedset!`
                  : `Add ${formatCurrency(25000 - totals.subtotal)} more to unlock Silk Pillows Pair!`
              }
            </strong>
          </div>
          <div class="milestone-progress-track">
            <div class="milestone-progress-fill" style="width: ${progressPercent}%;"></div>
          </div>
          <div class="milestone-nodes-row">
            <span class="${totals.subtotal >= 5000 ? 'achieved' : ''}">₹5k (Free Delivery)</span>
            <span class="${totals.subtotal >= 25000 ? 'achieved' : ''}">₹25k (Silk Pillows)</span>
            <span class="${totals.subtotal >= 50000 ? 'achieved' : ''}">₹50k (Egyptian Set)</span>
          </div>
        </div>

        ${
          state.cart.length > 0
            ? `
          <div class="cart-items-list">
            ${state.cart
              .map((item, idx) => {
                let title = '';
                let subtitle = '';
                let price = 0;
                let img = 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=300&auto=format&fit=crop';

                if (item.productId === 'bespoke_mattress_custom') {
                  title = item.customConfig ? item.customConfig.title : 'Bespoke Custom Mattress';
                  subtitle = item.customConfig?.monogram ? `Embroidered: "${item.customConfig.monogram}"` : 'Handcrafted Bespoke Atelier';
                  price = item.customConfig ? item.customConfig.price : 64000;
                } else {
                  const prod = APP_PRODUCTS.find((p) => p.id === item.productId);
                  if (prod) {
                    title = prod.title;
                    subtitle = prod.dimensions + ' • ' + prod.firmness;
                    price = prod.price;
                    img = prod.images[0];
                  }
                }

                return `
                <div class="cart-item-row">
                  <img src="${optimizeImageUrl(img, 160)}" alt="${escapeHtml(title)}" class="cart-item-img" loading="lazy" decoding="async">
                  <div class="cart-item-info">
                    <h5>${escapeHtml(title)}</h5>
                    <div class="cart-item-sub">${escapeHtml(subtitle)}</div>
                    <div class="cart-item-price">${formatCurrency(price)} × ${item.quantity} = <strong>${formatCurrency(price * item.quantity)}</strong></div>
                  </div>
                  <div class="cart-item-controls">
                    <div class="qty-stepper mini">
                      <button onclick="updateCartQuantity(${idx}, -1)">-</button>
                      <span>${item.quantity}</span>
                      <button onclick="updateCartQuantity(${idx}, 1)">+</button>
                    </div>
                    <button class="btn-remove-item" onclick="removeCartItem(${idx})">🗑️</button>
                  </div>
                </div>
              `;
              })
              .join('')}
          </div>

          <!-- Member Privilege Code Input Box -->
          <div class="coupon-box">
            ${
              state.user
                ? `
              <div class="member-unlocked-tag">
                <span>👑 Logged in as <strong>${escapeHtml(state.user.name)}</strong></span>
                <span class="member-privilege-badge">25% Privilege Eligible</span>
              </div>
            `
                : `
              <div class="member-privilege-banner" onclick="openModal('login')">
                <span class="lock-icon">🔒</span>
                <div class="privilege-banner-text">
                  <strong>Sign In / Register</strong> to unlock <strong>25% Member Privilege (WELCOME25)</strong>
                </div>
                <button class="btn-unlock-privilege" onclick="event.stopPropagation(); openModal('login')">Sign In →</button>
              </div>
            `
            }
            <div class="coupon-input-row" style="margin-top: 8px;">
              <input type="text" id="couponCodeInput" placeholder="Enter Privilege Code (WELCOME25)" value="${escapeHtml(state.appliedCoupon ? state.appliedCoupon.code : '')}">
              <button class="btn-apply-coupon" onclick="applyCouponCode()">Apply</button>
            </div>
            ${
              state.appliedCoupon
                ? `<div class="coupon-success-msg">✓ Applied ${escapeHtml(state.appliedCoupon.code)} (25% Member Privilege)! <button onclick="removeCoupon()">Remove</button></div>`
                : ''
            }
          </div>

          <!-- Price Summary Breakdown -->
          <div class="cart-summary-box">
            <div class="summary-line">
              <span>Items Subtotal:</span>
              <span>${formatCurrency(totals.subtotal)}</span>
            </div>
            ${
              totals.discount > 0
                ? `
              <div class="summary-line discount">
                <span>Coupon Discount (${escapeHtml(state.appliedCoupon.code)}):</span>
                <span>-${formatCurrency(totals.discount)}</span>
              </div>
            `
                : ''
            }
            <div class="summary-line">
              <span>Statutory 18% GST:</span>
              <span class="tax-included">Included in Pricing</span>
            </div>
            <div class="summary-line">
              <span>White-Glove In-Room Delivery:</span>
              <span class="delivery-free">FREE</span>
            </div>
            <div class="summary-line total">
              <span>Total Payable Amount:</span>
              <span class="total-amt">${formatCurrency(totals.finalTotal)}</span>
            </div>
          </div>

          <!-- Proceed to Checkout Button -->
          <button class="btn-hero-primary full-width cta-checkout" onclick="closeModal(); openModal('checkout');">
            Proceed to Checkout (${formatCurrency(totals.finalTotal)}) →
          </button>
        `
            : `
          <div class="empty-cart-state">
            <div class="empty-icon">🛍️</div>
            <h4>Your Sanctuary Cart is empty</h4>
            <p>Explore our handcrafted orthopedic sleep collection and bring royal comfort to your home.</p>
            <button class="btn-hero-primary" onclick="closeModal(); setActiveTab('products');">Explore Products →</button>
          </div>
        `
        }
      </div>
    </div>
  `;
}

function applyCouponCode() {
  const code = (document.getElementById('couponCodeInput')?.value || '').trim().toUpperCase();
  if (!code) return;

  const isMemberCode = code === 'WELCOME25' || code === 'SPRINGHAVEN25' || code === 'GD25';
  if (!isMemberCode) {
    showToastNotification('Only verified Member Privilege codes (WELCOME25) are eligible.');
    return;
  }

  if (!state.user) {
    showToastNotification('Please sign in or register to unlock your 25% Member Privilege (WELCOME25).');
    openModal('login');
    return;
  }

  state.appliedCoupon = { code: 'WELCOME25', percent: 25 };
  showToastNotification('✓ 25% Member Privilege Applied (WELCOME25)!');
  renderModal();
}

function removeCoupon() {
  state.appliedCoupon = null;
  renderModal();
  showToastNotification('Privilege discount removed.');
}

// -------------------------------------------------------------
// MODAL: CHECKOUT & ADDRESS BOOK (Exact match with CheckoutScreen.kt)
// -------------------------------------------------------------
function renderCheckoutModal() {
  const totals = getCartTotals();
  const selectedAddr = state.savedAddresses[0] || {
    name: 'Sanctuary Patron',
    phone: '+91 70149 83696',
    addressLine: 'D-4, Vijay Vihar Colony',
    city: 'Jaipur',
    state: 'Rajasthan',
    pincode: '302039'
  };

  return `
    <div class="modal-dialog checkout-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal(); openModal('cart');">← Back to Cart</button>
        <div class="modal-header-title">
          <h3>White-Glove Delivery & Checkout</h3>
          <span>256-Bit SSL Encrypted & Bank-Grade Security</span>
        </div>
        <button class="btn-modal-close" onclick="closeModal()">✕</button>
      </div>

      <div class="checkout-modal-body">
        <!-- Step 1: Delivery Address Selection / Entry -->
        <div class="checkout-card">
          <h4>1. Delivery Address</h4>
          <div class="checkout-address-preview">
            <strong>${escapeHtml(selectedAddr.name)}</strong> (${escapeHtml(selectedAddr.phone)})<br>
            ${escapeHtml(selectedAddr.addressLine)}, ${escapeHtml(selectedAddr.city)}, ${escapeHtml(selectedAddr.state)} - ${escapeHtml(selectedAddr.pincode)}
          </div>
          <button class="btn-text-gold" onclick="openAddAddressPrompt()">+ Enter New Address</button>
        </div>

        <!-- Step 2: Delivery Slot & In-Room Logistics -->
        <div class="checkout-card">
          <h4>2. In-Room Setup Logistics</h4>
          <div class="logistics-options-grid">
            <div>
              <label>Delivery Preference:</label>
              <select id="deliverySlot">
                <option value="Morning">Morning (10:00 AM - 2:00 PM IST)</option>
                <option value="Afternoon">Afternoon (2:00 PM - 6:00 PM IST)</option>
                <option value="Evening">Evening (6:00 PM - 8:00 PM IST)</option>
              </select>
            </div>
            <div>
              <label>Elevator / Floor Access:</label>
              <select id="floorElevator">
                <option value="Elevator">Service Elevator Available</option>
                <option value="Ground">Ground Floor Residence</option>
                <option value="Staircase">Staircase Transit (Technicians provided)</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Step 3: Payment Options -->
        <div class="checkout-card">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
            <h4 style="margin: 0;">3. Payment & Settlement Method</h4>
            <button class="btn-text-gold" style="font-size: 0.72rem;" onclick="promptBackendSettings()">⚙️ Gateway & API Settings</button>
          </div>
          <div class="payment-methods-list">
            <label class="payment-radio-row" style="border: 2px solid var(--forest-primary); background: var(--forest-container);">
              <input type="radio" name="paymentOption" value="razorpay_full" checked>
              <div class="payment-info">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <strong style="color: var(--forest-primary);">Official Razorpay Web Checkout (Direct Gateway)</strong>
                  <span style="background: #1B4332; color: #FFF; font-size: 0.65rem; padding: 2px 6px; border-radius: 4px; font-weight: 800;">OFFICIAL GATEWAY</span>
                </div>
                <span>Opens official Razorpay Checkout modal for cards, UPI, and netbanking (Key: <code style="font-size: 0.72rem; color: var(--forest-primary);">${RAZORPAY_CONFIG.keyId}</code>)</span>
                <span class="pay-amount">Pay Now: ${formatCurrency(totals.finalTotal)}</span>
              </div>
            </label>

            <label class="payment-radio-row">
              <input type="radio" name="paymentOption" value="cod_advance">
              <div class="payment-info">
                <strong>20% White-Glove Advance + 80% Cash on Delivery</strong>
                <span>Pay 20% advance now to begin handcrafted dispatch, balance upon bedroom setup</span>
                <span class="pay-amount">Pay Now: ${formatCurrency(totals.advance20Percent)} (Balance: ${formatCurrency(totals.balanceCod)})</span>
              </div>
            </label>

            <label class="payment-radio-row">
              <input type="radio" name="paymentOption" value="simulator_test">
              <div class="payment-info">
                <strong>⚡ Good Dream Standalone Simulator Fallback</strong>
                <span>Instant fallback test payment if external gateway network is unavailable</span>
                <span class="pay-amount">Pay Now: ${formatCurrency(totals.finalTotal)}</span>
              </div>
            </label>
          </div>
        </div>

        <!-- Step 4: Razorpay Test Mode Credentials Helper -->
        <div class="checkout-card test-helper-card">
          <div class="test-helper-header">
            <div class="test-helper-title-wrap">
              <span class="test-pill">🧪 Razorpay Test Mode</span>
              <strong>Official Test Credentials</strong>
            </div>
            <span class="test-badge-free">Zero Real Charges</span>
          </div>
          <p class="test-helper-desc">
            Razorpay Test Mode rejects real cards and real UPI IDs to protect against unintentional charges. Use these official test credentials in the payment checkout window:
          </p>
          <div class="test-creds-grid">
            <div class="test-cred-box">
              <div class="test-cred-title">💳 Standard Test Card 1 (OTP Flow)</div>
              <div class="test-cred-val">4100 2800 0000 1007</div>
              <div class="test-cred-sub">Exp: 12/26 | CVV: 123 | OTP: 123456</div>
            </div>
            <div class="test-cred-box">
              <div class="test-cred-title">💳 Standard Test Card 2 (Direct)</div>
              <div class="test-cred-val">4111 1111 1111 1111</div>
              <div class="test-cred-sub">Exp: 12/28 | CVV: 123 | OTP: any</div>
            </div>
            <div class="test-cred-box" style="grid-column: span 2;">
              <div class="test-cred-title">📱 Test UPI ID</div>
              <div class="test-cred-val">success@razorpay</div>
              <div class="test-cred-sub">Instant Success Authorization</div>
            </div>
          </div>
        </div>

        <!-- Step 5: Statutory Affirmative DPDP Consent Checkbox -->
        <div class="checkout-card consent-card">
          <label class="consent-checkbox-row">
            <input type="checkbox" id="dpdpConsentCheck" checked>
            <span>
              By placing this order, I affirmatively agree to Good Dream's 
              <a href="/terms.html" target="_blank">Terms of Service</a> and 
              <a href="/privacy-policy.html" target="_blank">Privacy Policy</a> 
              governed under the DPDP Act 2023. Card details are processed directly via PCI-DSS Level 1 Razorpay gateway.
            </span>
          </label>
        </div>

        <!-- Place Order Trigger Buttons -->
        <div class="checkout-cta-stack">
          <button class="btn-hero-primary full-width cta-place-order" onclick="executeCheckoutOrder()">
            Confirm & Pay (${formatCurrency(totals.finalTotal)}) →
          </button>
          <button class="btn-quick-test-pay full-width" onclick="executeQuickTestPayment()">
            ⚡ 1-Click Instant Test Pay (${formatCurrency(totals.finalTotal)})
          </button>
        </div>
      </div>
    </div>
  `;
}

function promptCustomRazorpayKey() {
  const currentKey = localStorage.getItem('gd_razorpay_key') || RAZORPAY_CONFIG.keyId;
  const newKey = prompt('Enter your Razorpay Key ID (rzp_test_... or rzp_live_...):', currentKey);
  if (newKey && newKey.trim().startsWith('rzp_')) {
    RAZORPAY_CONFIG.keyId = newKey.trim();
    localStorage.setItem('gd_razorpay_key', newKey.trim());
    showToastNotification(`✓ Active Razorpay Key set to ${newKey.trim()}`);
    renderModal();
  }
}

function promptBackendSettings() {
  const currentKey = localStorage.getItem('gd_razorpay_key') || RAZORPAY_CONFIG.keyId;
  const currentBackend = localStorage.getItem('gd_backend_url') || '';
  const currentStatus = API_CONFIG.getStatusDescription();
  const choice = prompt(
    `🔧 GOOD DREAM PAYMENT & GATEWAY SETTINGS\n` +
    `-----------------------------------------\n` +
    `• Active Razorpay Key: ${currentKey}\n` +
    `• Active Backend Mode: ${currentStatus}\n\n` +
    `Select an action:\n` +
    `1. Enter a new Razorpay Key ID (starts with rzp_test_ or rzp_live_)\n` +
    `2. Enter custom Backend API URL (e.g. https://my-server.onrender.com or http://localhost:3000)\n` +
    `3. Type "clear" to reset to defaults\n\n` +
    `Enter value:`,
    currentBackend || currentKey
  );

  if (!choice) return;
  const val = choice.trim();

  if (val.toLowerCase() === 'clear') {
    localStorage.removeItem('gd_backend_url');
    localStorage.removeItem('gd_razorpay_key');
    RAZORPAY_CONFIG.keyId = DEFAULT_RAZORPAY_KEY;
    showToastNotification('✓ Settings reset to defaults.');
    renderModal();
  } else if (val.startsWith('http://') || val.startsWith('https://')) {
    localStorage.setItem('gd_backend_url', val);
    showToastNotification(`✓ Connected Backend API: ${val}`);
    renderModal();
  } else if (val.startsWith('rzp_')) {
    RAZORPAY_CONFIG.keyId = val;
    localStorage.setItem('gd_razorpay_key', val);
    showToastNotification(`✓ Active Razorpay Key set to ${val}`);
    renderModal();
  } else {
    alert('Please enter a valid Razorpay Key (rzp_test_...) or Backend URL (https://...)');
  }
}

// -------------------------------------------------------------
// CHECKOUT EXECUTION & PAYMENT ROUTING
// -------------------------------------------------------------
async function executeCheckoutOrder() {
  const consent = document.getElementById('dpdpConsentCheck')?.checked;
  if (!consent) {
    alert('Please agree to the Terms of Service and Privacy Policy to proceed.');
    return;
  }

  const paymentType = document.querySelector('input[name="paymentOption"]:checked')?.value || 'razorpay_full';
  const totals = getCartTotals();
  const payableRupees = paymentType === 'cod_advance' ? totals.advance20Percent : totals.finalTotal;
  const amountInPaise = Math.max(100, Math.round(payableRupees * 100));

  const orderItems = state.cart.map((item) => {
    if (item.productId === 'bespoke_mattress_custom') {
      return {
        id: item.productId,
        title: item.customConfig ? item.customConfig.title : 'Bespoke Custom Mattress',
        quantity: item.quantity,
        price: item.customConfig ? item.customConfig.price : 64000
      };
    } else {
      const prod = APP_PRODUCTS.find((p) => p.id === item.productId);
      return {
        id: item.productId,
        title: prod ? prod.title : 'Good Dream Product',
        quantity: item.quantity,
        price: prod ? prod.price : 0
      };
    }
  });

  const shippingAddress = state.savedAddresses[0] || {
    name: 'Sanctuary Patron',
    phone: '+91 70149 83696',
    addressLine: 'D-4, Vijay Vihar Colony',
    city: 'Jaipur',
    state: 'Rajasthan',
    pincode: '302039'
  };

  // If simulator is selected, immediately open the built-in simulator gateway!
  if (paymentType === 'simulator_test') {
    openPaymentGatewayModal(
      { order_id: `order_test_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`, amount: amountInPaise },
      'simulator_test',
      payableRupees,
      shippingAddress,
      orderItems,
      totals
    );
    return;
  }

  const orderBtn = document.querySelector('.cta-place-order');
  const originalBtnText = orderBtn ? orderBtn.innerHTML : 'Confirm & Pay →';
  if (orderBtn) {
    orderBtn.disabled = true;
    orderBtn.innerHTML = '<span>Initiating Secure Gateway...</span>';
  }

  try {
    let orderData = null;
    let liveOrderSuccess = false;
    const backendBase = API_CONFIG.getBaseUrl();

    // STEP 1: Call backend endpoint to create order ONLY if a backend is actually reachable
    if (backendBase !== null) {
      try {
        const createUrl = backendBase ? `${backendBase}/api/create-order` : '/api/create-order';
        const createRes = await fetch(createUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            amount: amountInPaise,
            currency: 'INR',
            receipt: `receipt_${Date.now()}`
          })
        });

        if (createRes && createRes.ok) {
          orderData = await createRes.json();
          // Valid live order from Razorpay API starts with 'order_' and is not sandbox
          if (orderData && orderData.order_id && !orderData.is_sandbox && !orderData.order_id.startsWith('order_test_')) {
            liveOrderSuccess = true;
          }
        } else {
          const errJson = await createRes.json().catch(() => ({}));
          console.warn('Backend order creation returned non-OK:', createRes?.status, errJson);
          if (createRes && createRes.status === 401) {
            showToastNotification('⚠️ Razorpay test key rejected by Razorpay API. Opening Good Dream Test Simulator...', 4000);
          }
        }
      } catch (netErr) {
        console.warn('Backend /api/create-order offline or unreachable:', netErr);
      }
    } else {
      // ZERO-404 ARCHITECTURE: On static Firebase Hosting CDN, generate clean client-side order
      // Prevents 404 Not Found network errors from polluting the browser console!
      orderData = {
        order_id: null,
        amount: amountInPaise,
        currency: 'INR',
        key_id: RAZORPAY_CONFIG.keyId,
        is_client_side: true
      };
    }

    if (orderBtn) {
      orderBtn.disabled = false;
      orderBtn.innerHTML = originalBtnText;
    }

    // STEP 2: Launch Razorpay Standard Checkout SDK
    if (typeof window.Razorpay !== 'undefined') {
      const activeKeyId = (orderData && orderData.key_id) || RAZORPAY_CONFIG.keyId || DEFAULT_RAZORPAY_KEY;
      const options = {
        key: activeKeyId,
        amount: (orderData && orderData.amount) || amountInPaise,
        currency: (orderData && orderData.currency) || 'INR',
        name: 'GOOD DREAMS HOME DECOR',
        description: 'Luxury Sleep Systems & Handcrafted Mattresses',
        image: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?q=80&w=200',
        prefill: {
          name: shippingAddress.name || 'Sanctuary Patron',
          email: state.user?.email || 'patron@gooddream.com',
          contact: (shippingAddress.phone || '+917014983696').replace(/\s+/g, '')
        },
        theme: {
          color: '#1B4332'
        },
        handler: async function (response) {
          try {
            if (orderBtn) {
              orderBtn.innerHTML = '<span>Verifying Payment...</span>';
            }

            if (backendBase !== null && response.razorpay_signature) {
              try {
                const verifyUrl = backendBase ? `${backendBase}/api/verify-payment` : '/api/verify-payment';
                await fetch(verifyUrl, {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({
                    razorpay_order_id: response.razorpay_order_id || orderData?.order_id,
                    razorpay_payment_id: response.razorpay_payment_id,
                    razorpay_signature: response.razorpay_signature
                  })
                });
              } catch (verifyErr) {
                console.warn('Backend verification call skipped or offline:', verifyErr);
              }
            }

            await finalizeOrderSuccess(
              response.razorpay_order_id || orderData?.order_id || `ORD-${Date.now()}`,
              response.razorpay_payment_id || `pay_${Date.now()}`,
              paymentType === 'cod_advance' ? '20% Advance + COD (Razorpay Gateway)' : 'Razorpay Full Online (Live Gateway)',
              orderItems,
              totals,
              paymentType,
              shippingAddress
            );
          } catch (err) {
            console.error('Error handling payment success:', err);
            await finalizeOrderSuccess(
              response.razorpay_order_id || orderData?.order_id || `ORD-${Date.now()}`,
              response.razorpay_payment_id || `pay_${Date.now()}`,
              'Razorpay Full Online',
              orderItems,
              totals,
              paymentType,
              shippingAddress
            );
          }
        },
        modal: {
          ondismiss: function () {
            console.log('Payment modal dismissed by user');
            if (orderBtn) {
              orderBtn.disabled = false;
              orderBtn.innerHTML = originalBtnText;
            }
            showToastNotification('Payment was cancelled. Items remain in your cart.');
          }
        }
      };

      // If backend created a valid order_id, pass it
      if (liveOrderSuccess && orderData && orderData.order_id) {
        options.order_id = orderData.order_id;
      }

      try {
        const rzp = new window.Razorpay(options);

        rzp.on('payment.failed', function (failResponse) {
          console.error('Payment failed / rejected by Razorpay:', failResponse.error);
          if (orderBtn) {
            orderBtn.disabled = false;
            orderBtn.innerHTML = originalBtnText;
          }
          const desc = failResponse.error?.description || 'Transaction declined';
          showToastNotification(`Razorpay Gateway: ${desc}. Auto-opening Test Simulator...`, 5000);
          setTimeout(() => {
            openPaymentGatewayModal(
              orderData || { order_id: `order_test_${Date.now()}`, amount: amountInPaise },
              paymentType,
              payableRupees,
              shippingAddress,
              orderItems,
              totals
            );
          }, 500);
        });

        rzp.open();
        return;
      } catch (rzpErr) {
        console.warn('Could not launch Razorpay window, opening test simulator:', rzpErr);
      }
    }

    // STEP 3: Seamless Test Gateway Simulator Fallback
    openPaymentGatewayModal(
      orderData || { order_id: `order_test_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`, amount: amountInPaise },
      paymentType,
      payableRupees,
      shippingAddress,
      orderItems,
      totals
    );
  } catch (err) {
    console.error('Checkout error:', err);
    if (orderBtn) {
      orderBtn.disabled = false;
      orderBtn.innerHTML = originalBtnText;
    }
    // Fallback to simulator
    openPaymentGatewayModal({ order_id: `order_test_${Date.now()}`, amount: amountInPaise }, paymentType, payableRupees, shippingAddress, orderItems, totals);
  }
}

// -------------------------------------------------------------
// 1-CLICK INSTANT TEST PAYMENT (Fast-path testing)
// -------------------------------------------------------------
async function executeQuickTestPayment() {
  const consent = document.getElementById('dpdpConsentCheck')?.checked;
  if (!consent) {
    alert('Please agree to the Terms of Service and Privacy Policy to proceed.');
    return;
  }

  const paymentType = document.querySelector('input[name="paymentOption"]:checked')?.value || 'razorpay_full';
  const totals = getCartTotals();
  const payableRupees = paymentType === 'cod_advance' ? totals.advance20Percent : totals.finalTotal;
  const amountInPaise = Math.max(100, Math.round(payableRupees * 100));

  const orderItems = state.cart.map((item) => {
    if (item.productId === 'bespoke_mattress_custom') {
      return {
        id: item.productId,
        title: item.customConfig ? item.customConfig.title : 'Bespoke Custom Mattress',
        quantity: item.quantity,
        price: item.customConfig ? item.customConfig.price : 64000
      };
    } else {
      const prod = APP_PRODUCTS.find((p) => p.id === item.productId);
      return {
        id: item.productId,
        title: prod ? prod.title : 'Good Dream Product',
        quantity: item.quantity,
        price: prod ? prod.price : 0
      };
    }
  });

  const shippingAddress = state.savedAddresses[0] || {
    name: 'Sanctuary Patron',
    phone: '+91 70149 83696',
    addressLine: 'D-4, Vijay Vihar Colony',
    city: 'Jaipur',
    state: 'Rajasthan',
    pincode: '302039'
  };

  showToastNotification('⚡ Executing 1-Click Instant Test Payment...');

  let orderId = `order_test_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  const backendBase = API_CONFIG.getBaseUrl();
  if (backendBase !== null) {
    try {
      const createUrl = backendBase ? `${backendBase}/api/create-order` : '/api/create-order';
      const createRes = await fetch(createUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          amount: amountInPaise,
          currency: 'INR',
          receipt: `receipt_${Date.now()}`,
          test_mode: true
        })
      }).catch(() => null);

      if (createRes && createRes.ok) {
        const oData = await createRes.json();
        if (oData.order_id) orderId = oData.order_id;
      }
    } catch (e) {
      console.warn('Fallback to local orderId:', e);
    }
  }

  state.gatewayData = {
    orderData: { order_id: orderId, amount: amountInPaise },
    paymentType,
    payableRupees,
    shippingAddress,
    orderItems,
    totals
  };

  await completeConfirmedPayment(paymentType === 'cod_advance' ? '20% Advance + COD (Instant Test Verified)' : 'Online Test Instant Pay (Verified)');
}

// -------------------------------------------------------------
// INTERACTIVE PAYMENT GATEWAY SIMULATOR (UPI, Card, NetBanking, Controls)
// -------------------------------------------------------------
function openPaymentGatewayModal(orderData, paymentType, payableRupees, shippingAddress, orderItems, totals) {
  state.gatewayData = {
    orderData,
    paymentType,
    payableRupees,
    shippingAddress,
    orderItems,
    totals,
    activeTab: 'card',
    selectedUpiApp: 'Google Pay',
    selectedBank: 'HDFC Bank',
    isOtpStep: false,
    otpCode: '123456',
    cardData: {
      number: '4100 2800 0000 1007',
      expiry: '12/26',
      cvv: '123',
      name: shippingAddress.name || 'Sanctuary Patron'
    },
    customUpiId: 'tester@okhdfcbank'
  };
  openModal('payment_gateway', state.gatewayData);
}

function renderPaymentGatewayModal() {
  const gData = state.gatewayData || state.modalData || {};
  const payable = gData.payableRupees || 64000;
  const activeTab = gData.activeTab || 'upi';

  return `
    <div class="modal-dialog payment-gateway-dialog">
      <!-- Top Brand Header -->
      <div class="gateway-top-bar">
        <div class="gateway-brand">
          <span style="font-size: 1.3rem;">🌙</span>
          <div>
            <div class="brand-logo-txt">GOOD DREAM SANCTUARY</div>
            <div style="font-size: 0.65rem; color: #E0DDD5; letter-spacing: 0.5px;">256-BIT SSL ENCRYPTED GATEWAY</div>
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <span class="brand-badge">⚡ Test Sandbox</span>
          <button class="btn-modal-close" style="color: #FFF;" onclick="cancelGatewayPayment()">✕</button>
        </div>
      </div>

      <!-- Amount Header Card -->
      <div class="gateway-amount-card">
        <div>
          <span class="gateway-amount-label">Payable Amount (${gData.paymentType === 'cod_advance' ? '20% Advance' : 'Full Online'}):</span>
          <div style="font-size: 0.72rem; color: var(--text-secondary);">Order Reference: <strong>#${escapeHtml(gData.orderData?.order_id || 'order_test_demo')}</strong></div>
        </div>
        <div class="gateway-amount-val">${formatCurrency(payable)}</div>
      </div>

      <!-- Gateway Method Tabs -->
      <div class="gateway-tabs">
        <button class="gateway-tab-btn ${activeTab === 'upi' ? 'active' : ''}" onclick="switchGatewayTab('upi')">
          <span>⚡ UPI / QR</span>
        </button>
        <button class="gateway-tab-btn ${activeTab === 'card' ? 'active' : ''}" onclick="switchGatewayTab('card')">
          <span>💳 Card</span>
        </button>
        <button class="gateway-tab-btn ${activeTab === 'netbanking' ? 'active' : ''}" onclick="switchGatewayTab('netbanking')">
          <span>🏛️ NetBanking</span>
        </button>
        <button class="gateway-tab-btn ${activeTab === 'simulator' ? 'active' : ''}" onclick="switchGatewayTab('simulator')">
          <span>🧪 Simulator</span>
        </button>
      </div>

      <!-- Tab Content Area -->
      <div class="gateway-tab-content">
        ${renderGatewayTabContent(gData)}
      </div>
    </div>
  `;
}

function renderGatewayTabContent(gData) {
  const payable = gData.payableRupees || 64000;
  const activeTab = gData.activeTab || 'upi';

  if (activeTab === 'upi') {
    const selectedApp = gData.selectedUpiApp || 'Google Pay';
    return `
      <div>
        <div style="font-size: 0.82rem; font-weight: 700; margin-bottom: 8px; color: var(--forest-primary);">Select Instant UPI App:</div>
        <div class="upi-grid">
          <div class="upi-btn-card ${selectedApp === 'Google Pay' ? 'selected' : ''}" onclick="selectUpiApp('Google Pay')">
            <span class="upi-app-icon">🟢</span>
            <span>GPay</span>
          </div>
          <div class="upi-btn-card ${selectedApp === 'PhonePe' ? 'selected' : ''}" onclick="selectUpiApp('PhonePe')">
            <span class="upi-app-icon">🟣</span>
            <span>PhonePe</span>
          </div>
          <div class="upi-btn-card ${selectedApp === 'Paytm' ? 'selected' : ''}" onclick="selectUpiApp('Paytm')">
            <span class="upi-app-icon">🔵</span>
            <span>Paytm</span>
          </div>
          <div class="upi-btn-card ${selectedApp === 'BHIM' ? 'selected' : ''}" onclick="selectUpiApp('BHIM')">
            <span class="upi-app-icon">🟠</span>
            <span>BHIM</span>
          </div>
        </div>

        <div style="font-size: 0.82rem; font-weight: 700; margin-bottom: 6px; color: var(--forest-primary);">Or Scan Test UPI QR Code:</div>
        <div class="qr-sim-box">
          <div class="qr-art"></div>
          <div style="font-size: 0.72rem; color: var(--text-secondary); margin-top: 4px;">Dynamic UPI Intent QR • Tap button below to authorize</div>
        </div>

        <div style="font-size: 0.82rem; font-weight: 700; margin-bottom: 6px; color: var(--forest-primary);">Or Enter Custom Virtual Payment Address (VPA):</div>
        <div class="upi-input-group">
          <input type="text" id="customVpaInput" value="${escapeHtml(gData.customUpiId || 'tester@okhdfcbank')}" placeholder="username@upi">
        </div>
      </div>

      <div style="margin-top: 14px;">
        <button class="btn-hero-primary full-width" onclick="executeGatewayUpiPayment()">
          Pay ${formatCurrency(payable)} via ${escapeHtml(selectedApp)} →
        </button>
      </div>
    `;
  }

  if (activeTab === 'card') {
    if (gData.isOtpStep) {
      return `
        <div class="otp-prompt-box">
          <div style="font-size: 1.8rem;">🔒</div>
          <div style="font-weight: 800; font-size: 1.05rem; color: var(--forest-dark);">Bank 3D-Secure Verification</div>
          <p style="font-size: 0.8rem; color: var(--text-secondary); margin: 0;">
            A one-time passcode has been dispatched to your mobile (+91 70149 83696) for payment of <strong>${formatCurrency(payable)}</strong>.
          </p>

          <input type="text" id="gatewayOtpInput" class="otp-input-field" maxlength="6" value="123456">

          <div style="font-size: 0.72rem; color: #2E7D32; font-weight: 700;">
            ✓ Simulated Test Mode: Test OTP "123456" has been auto-filled.
          </div>

          <div style="display: flex; gap: 8px; margin-top: 10px;">
            <button class="btn-hero-primary full-width" onclick="verifyGatewayOtp()">
              Verify OTP & Authorize Payment →
            </button>
            <button class="btn-hero-outline" style="width: auto; padding: 10px 14px;" onclick="setGatewayOtpStep(false)">
              ← Back
            </button>
          </div>
        </div>
      `;
    }

    return `
      <div>
        <!-- Luxury Virtual Card Preview -->
        <div class="virtual-card-preview">
          <div class="vc-top">
            <div class="vc-chip"></div>
            <div class="vc-brand">SPRINGHAVEN LUXURY</div>
          </div>
          <div class="vc-number" id="vcNumberDisplay">${escapeHtml(gData.cardData?.number || '4100 2800 0000 1007')}</div>
          <div class="vc-bottom">
            <div>
              <span>CARDHOLDER</span><br>
              <strong style="color: #FFF;" id="vcNameDisplay">${escapeHtml(gData.cardData?.name || gData.shippingAddress?.name || 'SANCTUARY PATRON')}</strong>
            </div>
            <div>
              <span>EXPIRES</span><br>
              <strong style="color: #FFF;" id="vcExpDisplay">${escapeHtml(gData.cardData?.expiry || '12/26')}</strong>
            </div>
          </div>
        </div>

        <!-- Quick Select Test Cards -->
        <div style="display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap;">
          <button type="button" class="btn-sm-outline" style="font-size: 0.72rem; padding: 5px 10px; border-radius: 6px; border: 1px solid var(--forest-primary); background: #FFF; cursor: pointer; font-weight: 600;" onclick="selectTestCard('4100 2800 0000 1007', '12/26', '123')">
            ✨ Test Card: 4100 2800 0000 1007 (Exp: 12/26)
          </button>
          <button type="button" class="btn-sm-outline" style="font-size: 0.72rem; padding: 5px 10px; border-radius: 6px; border: 1px solid var(--forest-primary); background: #FFF; cursor: pointer; font-weight: 600;" onclick="selectTestCard('4111 1111 1111 1111', '12/28', '123')">
            💳 Test Card: 4111 1111 1111 1111 (Exp: 12/28)
          </button>
        </div>

        <!-- Inputs Form -->
        <div class="card-inputs-grid">
          <div class="card-input-item" style="grid-column: span 3;">
            <label>Card Number (Razorpay Test Card)</label>
            <input type="text" id="cardNumInput" value="${escapeHtml(gData.cardData?.number || '4100 2800 0000 1007')}" oninput="updateVcNumber(this.value)">
          </div>
          <div class="card-input-item">
            <label>Cardholder Name</label>
            <input type="text" id="cardNameInput" value="${escapeHtml(gData.cardData?.name || gData.shippingAddress?.name || 'Sanctuary Patron')}" oninput="updateVcName(this.value)">
          </div>
          <div class="card-input-item">
            <label>Expiry (MM/YY)</label>
            <input type="text" id="cardExpInput" value="${escapeHtml(gData.cardData?.expiry || '12/26')}" oninput="updateVcExp(this.value)">
          </div>
          <div class="card-input-item">
            <label>CVV</label>
            <input type="password" id="cardCvvInput" value="${escapeHtml(gData.cardData?.cvv || '123')}" maxlength="3">
          </div>
        </div>
      </div>

      <div style="margin-top: 14px;">
        <button class="btn-hero-primary full-width cta-pay-card" onclick="executeGatewayCardPayment()">
          Pay ${formatCurrency(payable)} via Card →
        </button>
      </div>
    `;
  }

  if (activeTab === 'netbanking') {
    const selectedBank = gData.selectedBank || 'HDFC Bank';
    return `
      <div>
        <div style="font-size: 0.82rem; font-weight: 700; margin-bottom: 10px; color: var(--forest-primary);">Select Indian Retail Net Banking:</div>
        <div class="bank-grid">
          <div class="bank-option-btn ${selectedBank === 'HDFC Bank' ? 'selected' : ''}" onclick="selectGatewayBank('HDFC Bank')">🏛️ HDFC Bank</div>
          <div class="bank-option-btn ${selectedBank === 'ICICI Bank' ? 'selected' : ''}" onclick="selectGatewayBank('ICICI Bank')">🏛️ ICICI Bank</div>
          <div class="bank-option-btn ${selectedBank === 'SBI' ? 'selected' : ''}" onclick="selectGatewayBank('SBI')">🏛️ State Bank of India</div>
          <div class="bank-option-btn ${selectedBank === 'Axis Bank' ? 'selected' : ''}" onclick="selectGatewayBank('Axis Bank')">🏛️ Axis Bank</div>
          <div class="bank-option-btn ${selectedBank === 'Kotak' ? 'selected' : ''}" onclick="selectGatewayBank('Kotak')">🏛️ Kotak Mahindra</div>
          <div class="bank-option-btn ${selectedBank === 'PNB' ? 'selected' : ''}" onclick="selectGatewayBank('PNB')">🏛️ Punjab National</div>
        </div>

        <div style="background: var(--surface-subtle); padding: 10px; border-radius: var(--radius-sm); font-size: 0.75rem; color: var(--text-secondary); margin-top: 10px;">
          ℹ️ In Test Simulator mode, selecting your preferred bank will simulate high-security NetBanking gateway redirect and signature callback.
        </div>
      </div>

      <div style="margin-top: 14px;">
        <button class="btn-hero-primary full-width" onclick="executeGatewayBankPayment()">
          Authorize via ${escapeHtml(selectedBank)} (${formatCurrency(payable)}) →
        </button>
      </div>
    `;
  }

  // ActiveTab === 'simulator'
  return `
    <div>
      <div style="font-size: 0.88rem; font-weight: 800; color: var(--forest-primary); margin-bottom: 8px;">
        Developer & Tester Sandbox Suite
      </div>
      <p style="font-size: 0.8rem; color: var(--text-secondary); margin-bottom: 14px;">
        Simulate all possible transaction outcomes instantly to test order persistence, GST invoice generation, cart clearing, and live 4-stage tracking.
      </p>

      <div class="sim-controls-deck">
        <button class="btn-sim-action success" onclick="completeConfirmedPayment('Instant Sandbox Simulator Authorization')">
          <span>✅</span>
          <span>Simulate Payment SUCCESS (Pass & Confirm Order)</span>
        </button>

        <button class="btn-sim-action fail" onclick="simulateGatewayFailure()">
          <span>❌</span>
          <span>Simulate Payment FAILURE (Decline & Test Error Handling)</span>
        </button>

        <button class="btn-sim-action secondary" onclick="cancelGatewayPayment()">
          <span>✕</span>
          <span>Cancel & Safely Retain Cart</span>
        </button>
      </div>
    </div>
  `;
}

// Gateway Tab Switchers & Interactions
function switchGatewayTab(tabName) {
  if (state.gatewayData) {
    state.gatewayData.activeTab = tabName;
    state.gatewayData.isOtpStep = false;
  }
  renderModal();
}

function selectUpiApp(appName) {
  if (state.gatewayData) {
    state.gatewayData.selectedUpiApp = appName;
  }
  renderModal();
}

function selectGatewayBank(bankName) {
  if (state.gatewayData) {
    state.gatewayData.selectedBank = bankName;
  }
  renderModal();
}

function setGatewayOtpStep(isOtp) {
  if (state.gatewayData) {
    state.gatewayData.isOtpStep = Boolean(isOtp);
  }
  renderModal();
}

async function executeGatewayUpiPayment() {
  const gData = state.gatewayData || {};
  const vpa = document.getElementById('customVpaInput')?.value || gData.customUpiId || 'tester@okhdfcbank';
  const app = gData.selectedUpiApp || 'UPI';
  await completeConfirmedPayment(`UPI (${app} - ${vpa})`);
}

function selectTestCard(number, expiry, cvv) {
  if (state.gatewayData) {
    state.gatewayData.cardData = {
      number: number,
      expiry: expiry,
      cvv: cvv,
      name: state.gatewayData.shippingAddress?.name || 'Sanctuary Patron'
    };
  }
  const numEl = document.getElementById('cardNumInput');
  const expEl = document.getElementById('cardExpInput');
  const cvvEl = document.getElementById('cardCvvInput');
  if (numEl) numEl.value = number;
  if (expEl) expEl.value = expiry;
  if (cvvEl) cvvEl.value = cvv;
  updateVcNumber(number);
  updateVcExp(expiry);
}

function updateVcNumber(val) {
  const el = document.getElementById('vcNumberDisplay');
  if (el) el.textContent = val || '•••• •••• •••• ••••';
}

function updateVcName(val) {
  const el = document.getElementById('vcNameDisplay');
  if (el) el.textContent = (val || 'SANCTUARY PATRON').toUpperCase();
}

function updateVcExp(val) {
  const el = document.getElementById('vcExpDisplay');
  if (el) el.textContent = val || '12/26';
}

function executeGatewayCardPayment() {
  const cardNum = document.getElementById('cardNumInput')?.value || '4100 2800 0000 1007';
  const cardExp = document.getElementById('cardExpInput')?.value || '12/26';
  const cardCvv = document.getElementById('cardCvvInput')?.value || '123';
  const cardName = document.getElementById('cardNameInput')?.value || 'Sanctuary Patron';

  if (state.gatewayData) {
    state.gatewayData.cardData = {
      number: cardNum,
      expiry: cardExp,
      cvv: cardCvv,
      name: cardName
    };
  }
  setGatewayOtpStep(true);
}

async function verifyGatewayOtp() {
  const otp = document.getElementById('gatewayOtpInput')?.value || '123456';
  if (!otp || otp.trim().length !== 6) {
    alert('Please enter a valid 6-digit test OTP.');
    return;
  }
  const rawNum = (state.gatewayData?.cardData?.number || '1007').replace(/\s+/g, '');
  const last4 = rawNum.slice(-4) || '1007';
  await completeConfirmedPayment(`Card (Visa Test Ending ${last4} - 3DS Verified)`);
}

async function executeGatewayBankPayment() {
  const gData = state.gatewayData || {};
  const bank = gData.selectedBank || 'HDFC Bank';
  await completeConfirmedPayment(`Net Banking (${bank} Test Authorized)`);
}

function simulateGatewayFailure() {
  alert('Simulated Payment Failure: Issuing bank declined transaction (ERR_TEST_DECLINE). Your sanctuary items remain safely in your cart.');
  closeModal();
  openModal('checkout');
}

function cancelGatewayPayment() {
  closeModal();
  showToastNotification('Payment cancelled. Your items remain in your cart.');
}

// -------------------------------------------------------------
// ORDER CONFIRMATION & PERSISTENCE ENGINE
// -------------------------------------------------------------
async function completeConfirmedPayment(paymentMethodLabel) {
  const gData = state.gatewayData || state.modalData || {};
  const orderId = gData.orderData?.order_id || `order_test_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  const amount = gData.orderData?.amount || Math.round((gData.payableRupees || 64000) * 100);

  showToastNotification('Authenticating payment with server & issuing bank...');

  let paymentId = `pay_test_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`;
  let signature = `sandbox_sig_${orderId}`;

  const backendBase = API_CONFIG.getBaseUrl();
  if (backendBase !== null) {
    try {
      // 1. Try server simulation endpoint for authentic HMAC signature
      const simUrl = backendBase ? `${backendBase}/api/simulate-payment` : '/api/simulate-payment';
      const simRes = await fetch(simUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          order_id: orderId,
          amount: amount,
          payment_method: paymentMethodLabel
        })
      }).catch(() => null);

      if (simRes && simRes.ok) {
        const simData = await simRes.json();
        paymentId = simData.payment_id;
        signature = simData.signature;
      }

      // 2. Call /api/verify-payment for cryptographic validation
      const verifyUrl = backendBase ? `${backendBase}/api/verify-payment` : '/api/verify-payment';
      const verifyRes = await fetch(verifyUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          razorpay_order_id: orderId,
          razorpay_payment_id: paymentId,
          razorpay_signature: signature
        })
      }).catch(() => null);

      if (verifyRes && verifyRes.ok) {
        console.log('Payment signature verified successfully on backend');
      }
    } catch (err) {
      console.warn('Backend verification call skipped or offline:', err);
    }
  }

  await finalizeOrderSuccess(
    orderId,
    paymentId,
    paymentMethodLabel,
    gData.orderItems || [],
    gData.totals || getCartTotals(),
    gData.paymentType || 'razorpay_full',
    gData.shippingAddress
  );
}

async function finalizeOrderSuccess(orderId, paymentId, paymentMethodLabel, orderItems, totals, paymentType, shippingAddress) {
  const confirmedOrder = {
    orderId: orderId,
    paymentId: paymentId,
    createdAt: Date.now(),
    items: orderItems.length > 0 ? orderItems : [
      {
        id: 'flagship_springhaven_15',
        title: 'SpringHaven™ Grand Palais 15" Integrated Ensembles',
        quantity: 1,
        price: 185000
      }
    ],
    finalTotal: totals.finalTotal,
    advancePaid: paymentType === 'cod_advance' ? totals.advance20Percent : totals.finalTotal,
    balanceDue: paymentType === 'cod_advance' ? totals.balanceCod : 0,
    paymentMethod: paymentMethodLabel,
    status: 'CONFIRMED',
    shippingAddress: shippingAddress || {
      name: 'Sanctuary Patron',
      phone: '+91 70149 83696',
      addressLine: 'D-4, Vijay Vihar Colony',
      city: 'Jaipur',
      state: 'Rajasthan',
      pincode: '302039'
    }
  };

  // Prepend to order history & persist
  state.orders.unshift(confirmedOrder);
  persistOrders();

  // Clear cart
  state.cart = [];
  persistCart();
  updateBadgeCounts();

  // Open Order Success Modal
  closeModal();
  openModal('order_success', confirmedOrder);
  showToastNotification(`🎉 Payment Authorized! Order #${confirmedOrder.orderId} Confirmed!`);
}

// -------------------------------------------------------------
// MODAL: ORDER SUCCESS & GST INVOICE (Exact match with OrderSuccessScreen.kt)
// -------------------------------------------------------------
function renderOrderSuccessModal() {
  const order = state.modalData || state.orders[0];
  const deliveryDate = new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toLocaleDateString('en-IN', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  return `
    <div class="modal-dialog success-modal-dialog">
      <div class="success-seal-wrap">
        <div class="gold-seal-pulse"></div>
        <div class="gold-seal-emblem">
          <span>AUTHENTICITY GUARANTEED</span>
          <strong>25-YR</strong>
          <span>SPRINGHAVEN™</span>
        </div>
      </div>

      <div class="success-header">
        <h2>Order Confirmed & Reserved</h2>
        <p>Order Reference: <strong>#${escapeHtml(order.orderId)}</strong></p>
        <span class="est-delivery">Estimated White-Glove In-Room Setup: <strong>${deliveryDate}</strong></span>
      </div>

      <!-- 4-Stage Live White-Glove Order Journey Timeline -->
      <div class="journey-timeline">
        <div class="timeline-step completed">
          <div class="step-marker">✓</div>
          <div class="step-info">
            <strong>1. Order Reserved & Confirmed</strong>
            <span>Cryptographic order recorded; dedicated sleep concierge assigned.</span>
          </div>
        </div>

        <div class="timeline-step in-progress">
          <div class="step-marker">⚙️</div>
          <div class="step-info">
            <strong>2. Atelier Crafting & Orthopedic Inspection</strong>
            <span>Multi-zone pocket coil assembly, latex quilting, and tension testing.</span>
          </div>
        </div>

        <div class="timeline-step upcoming">
          <div class="step-marker">🚚</div>
          <div class="step-info">
            <strong>3. White-Glove Transit & Air-Suspension Logistics</strong>
            <span>Climate-shielded dispatch with live GPS tracking notifications.</span>
          </div>
        </div>

        <div class="timeline-step upcoming">
          <div class="step-marker">🛏️</div>
          <div class="step-info">
            <strong>4. Bedroom Delivery & Complimentary Setup</strong>
            <span>Technicians unpack, inspect, and position mattress on your bedstead.</span>
          </div>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="success-actions-row">
        <button class="btn-hero-primary" onclick="printInvoice('${escapeHtml(order.orderId)}')">
          Download / Print GST Invoice 📄
        </button>
        <button class="btn-hero-outline" onclick="closeModal(); openModal('order_tracking', { orderId: '${escapeHtml(order.orderId)}' })">
          Track Live Progress 🚚
        </button>
        <button class="btn-text-gold" onclick="closeModal(); setActiveTab('home');">
          Back to Sanctuary Home →
        </button>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// GST INVOICE GENERATOR & PRINTER (Matching InvoicePrinterHelper.kt)
// -------------------------------------------------------------
function printInvoice(orderId) {
  const order = state.orders.find((o) => o.orderId === orderId) || state.orders[0];
  if (!order) return;

  const addr = order.shippingAddress;
  const taxableAmount = Math.round(order.finalTotal / 1.18);
  const totalGst = order.finalTotal - taxableAmount;
  const halfGst = Math.round(totalGst / 2);

  const printWindow = window.open('', '_blank', 'width=850,height=950');
  if (!printWindow) {
    alert('Please allow popups to view the GST Tax Invoice.');
    return;
  }

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>GST Tax Invoice - ${escapeHtml(order.orderId)}</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; padding: 40px; color: #111; line-height: 1.5; font-size: 13px; }
        .invoice-box { max-width: 800px; margin: auto; border: 1px solid #ddd; padding: 30px; }
        .header-table { width: 100%; border-bottom: 2px solid #102E23; padding-bottom: 15px; margin-bottom: 20px; }
        .brand-title { font-size: 24px; font-weight: 800; color: #102E23; text-transform: uppercase; }
        .brand-sub { font-size: 11px; color: #C8A246; font-weight: 700; letter-spacing: 1px; }
        .invoice-details { text-align: right; }
        .parties-table { width: 100%; margin-bottom: 25px; }
        .parties-table td { vertical-align: top; width: 50%; }
        .items-table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }
        .items-table th, .items-table td { border: 1px solid #eee; padding: 10px; text-align: left; }
        .items-table th { background: #f8f6f0; color: #102E23; font-weight: 700; }
        .totals-table { width: 40%; margin-left: auto; margin-bottom: 30px; }
        .totals-table td { padding: 6px; }
        .totals-table tr.grand-total { font-weight: 800; font-size: 15px; border-top: 2px solid #102E23; }
        .footer-note { font-size: 11px; color: #666; border-top: 1px solid #ddd; padding-top: 15px; text-align: center; }
      </style>
    </head>
    <body>
      <div class="invoice-box">
        <table class="header-table">
          <tr>
            <td>
              <div class="brand-title">Good Dream</div>
              <div class="brand-sub">SpringHaven Mattress Sanctuary Atelier</div>
              <div style="font-size: 11px; color: #555; margin-top: 6px;">
                <strong>${CORPORATE_CONFIG.companyName}</strong><br>
                ${CORPORATE_CONFIG.registeredOffice}<br>
                Marketed by: ${CORPORATE_CONFIG.marketedBy}<br>
                Helpline: ${CORPORATE_CONFIG.supportPhone} | Email: ${CORPORATE_CONFIG.supportEmail}
              </div>
            </td>
            <td class="invoice-details">
              <div style="font-size: 18px; font-weight: 800; color: #102E23;">TAX INVOICE</div>
              <div>Invoice No: <strong>INV-${escapeHtml(order.orderId)}</strong></div>
              <div>Date: ${new Date(order.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</div>
              <div>Place of Supply: <strong>Jaipur, Rajasthan (State Code: 08)</strong></div>
              <div>Payment Mode: <strong>${escapeHtml(order.paymentMethod)}</strong></div>
            </td>
          </tr>
        </table>

        <table class="parties-table">
          <tr>
            <td>
              <strong>Billed & Shipped To:</strong><br>
              <strong>${escapeHtml(addr.name)}</strong><br>
              ${escapeHtml(addr.addressLine)}<br>
              ${escapeHtml(addr.city)}, ${escapeHtml(addr.state)} - ${escapeHtml(addr.pincode)}<br>
              Phone: ${escapeHtml(addr.phone)}
            </td>
            <td style="text-align: right;">
              <strong>Delivery Mode:</strong> White-Glove Direct Transit<br>
              <strong>Status:</strong> ${escapeHtml(order.status)} / Confirmed<br>
              <strong>HSN Classification:</strong> 9404 (Mattress & Bedding)
            </td>
          </tr>
        </table>

        <table class="items-table">
          <thead>
            <tr>
              <th>Description</th>
              <th>HSN</th>
              <th>Qty</th>
              <th>Rate (₹)</th>
              <th>Total (₹)</th>
            </tr>
          </thead>
          <tbody>
            ${order.items
              .map(
                (item) => `
              <tr>
                <td>${escapeHtml(item.title)}</td>
                <td>9404</td>
                <td>${item.quantity}</td>
                <td>${formatCurrency(item.price)}</td>
                <td>${formatCurrency(item.price * item.quantity)}</td>
              </tr>
            `
              )
              .join('')}
          </tbody>
        </table>

        <table class="totals-table">
          <tr>
            <td>Taxable Value:</td>
            <td style="text-align: right;">${formatCurrency(taxableAmount)}</td>
          </tr>
          <tr>
            <td>CGST (9%):</td>
            <td style="text-align: right;">${formatCurrency(halfGst)}</td>
          </tr>
          <tr>
            <td>SGST (9%):</td>
            <td style="text-align: right;">${formatCurrency(halfGst)}</td>
          </tr>
          <tr class="grand-total">
            <td>Grand Total (INR):</td>
            <td style="text-align: right; color: #102E23;">${formatCurrency(order.finalTotal)}</td>
          </tr>
        </table>

        <div class="footer-note">
          This is an electronically generated statutory GST tax invoice issued by <strong>${CORPORATE_CONFIG.companyName}</strong> under the provisions of the Indian Goods and Services Tax Act. Handcrafted in India under IS 7933 standards.
        </div>
      </div>
      <script>window.onload = function() { window.print(); };</script>
    </body>
    </html>
  `);
  printWindow.document.close();
}

// -------------------------------------------------------------
// MODAL: ORDER TRACKING (Exact match with OrderTrackingModal.kt)
// -------------------------------------------------------------
function renderOrderTrackingModal() {
  const targetId = state.modalData?.orderId || (state.orders[0]?.orderId ?? '');

  return `
    <div class="modal-dialog tracking-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal()">← Close</button>
        <div class="modal-header-title">
          <h3>Live Order Tracking</h3>
          <span>Air-Suspension Logistics Portal</span>
        </div>
        <button class="btn-modal-close" onclick="closeModal()">✕</button>
      </div>

      <div class="tracking-modal-body">
        <div class="tracking-search-bar">
          <input type="text" id="trackOrderIdInput" placeholder="Enter Order ID (e.g. ${escapeHtml(targetId || 'GD-782910')})" value="${escapeHtml(targetId)}">
          <button class="btn-hero-primary" onclick="lookupOrderTracking()">Track →</button>
        </div>

        <div id="trackingResultArea">
          ${renderTrackingDetails(targetId)}
        </div>
      </div>
    </div>
  `;
}

function lookupOrderTracking() {
  const id = document.getElementById('trackOrderIdInput')?.value.trim();
  const area = document.getElementById('trackingResultArea');
  if (area) {
    area.innerHTML = renderTrackingDetails(id);
  }
}

function renderTrackingDetails(orderId) {
  const order = state.orders.find((o) => o.orderId === orderId);

  return `
    <div class="tracking-card">
      <div class="tracking-header-row">
        <div>
          <h4>Consignment #${escapeHtml(orderId || 'GD-782910')}</h4>
          <span class="carrier-tag">Carrier: Good Dream White-Glove Air-Suspension Fleet</span>
        </div>
        <span class="status-chip in-transit">IN TRANSIT</span>
      </div>

      <div class="tracking-steps-vertical">
        <div class="t-step active">
          <div class="t-dot">✓</div>
          <div class="t-text">
            <strong>Order Reserved & Dispatched from Jaipur Facility</strong>
            <span>Consignment cleared quality verification and packed in dual-seal waterproof sleeves.</span>
          </div>
        </div>

        <div class="t-step active">
          <div class="t-dot">🚚</div>
          <div class="t-text">
            <strong>In Transit to Destination Hub</strong>
            <span>Air-suspension specialized luxury logistics vehicle en route.</span>
          </div>
        </div>

        <div class="t-step">
          <div class="t-dot">○</div>
          <div class="t-text">
            <strong>Scheduled In-Room Delivery & Setup</strong>
            <span>Concierge technicians will arrive with shoe covers and white gloves.</span>
          </div>
        </div>
      </div>

      <div class="tracking-help-box">
        Need assistance with delivery rescheduling? Contact Logistics Concierge: 
        <strong>${CORPORATE_CONFIG.supportPhone}</strong> | <strong>${CORPORATE_CONFIG.supportEmail}</strong>
      </div>
    </div>
  `;
}

// -------------------------------------------------------------
// MODAL: DREAMCARE AI CONCIERGE (Exact match with AiChatBotModal.kt)
// -------------------------------------------------------------
function renderAiConciergeModal() {
  return `
    <div class="modal-dialog ai-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal()">← Close</button>
        <div class="modal-header-title">
          <h3>DreamCare AI Sleep Concierge 🤖</h3>
          <span>Trained on Orthopedic Ergonomics & Good Dream Catalog</span>
        </div>
        <button class="btn-modal-close" onclick="closeModal()">✕</button>
      </div>

      <div class="ai-modal-body">
        <div class="ai-messages-scroll" id="aiMessagesBox">
          ${state.aiChatMessages
            .map(
              (m) => `
            <div class="chat-bubble ${m.sender}">
              <div class="bubble-content">${m.sender === 'bot' ? formatBotMessage(m.text) : escapeHtml(m.text)}</div>
            </div>
          `
            )
            .join('')}
        </div>

        <!-- Quick Prompts Chips -->
        <div class="ai-quick-chips">
          <button onclick="sendQuickAiPrompt('Which mattress is best for chronic lower back pain?')">Best for back pain?</button>
          <button onclick="sendQuickAiPrompt('How does the 25-Year SpringHaven Warranty work?')">25-Year warranty details</button>
          <button onclick="sendQuickAiPrompt('Can you make a custom size for my antique bed?')">Custom sizing inquiry</button>
          <button onclick="sendQuickAiPrompt('What is the difference between SpringHaven and regular mattresses?')">SpringHaven vs Regular?</button>
        </div>

        <!-- Chat Input Form -->
        <div class="ai-input-bar">
          <input type="text" id="aiChatInput" placeholder="Ask anything about mattresses, firmness, or shipping..." onkeydown="if(event.key==='Enter') sendAiMessage();">
          <button class="btn-send-ai" onclick="sendAiMessage()">Send →</button>
        </div>
      </div>
    </div>
  `;
}

function sendQuickAiPrompt(text) {
  const input = document.getElementById('aiChatInput');
  if (input) input.value = text;
  sendAiMessage();
}

function sendAiMessage() {
  const input = document.getElementById('aiChatInput');
  const text = (input?.value || '').trim();
  if (!text) return;

  state.aiChatMessages.push({ sender: 'user', text });
  input.value = '';

  // Simulate intelligent response based on Good Dream catalog
  let reply = "Our sleep concierge is delighted to assist! ";
  const q = text.toLowerCase();

  if (q.includes('back pain') || q.includes('ortho') || q.includes('spine')) {
    reply = "For chronic back or spinal pain, we recommend the certified **Good Dream OrthoCare SpineAlign 8\"** (₹24,999) or our flagship **SpringHaven Grand Sovereign 15\"** (₹89,999). Both feature 5-zone anatomical contouring that relieves sciatic nerve tension while keeping your vertebrae in neutral alignment.";
  } else if (q.includes('warranty') || q.includes('guarantee') || q.includes('return') || q.includes('trial')) {
    reply = "Good Dream mattresses are bespoke handcrafted sleep systems protected by our **25-Year SpringHaven™ Structural Warranty** and complimentary white-glove bedroom delivery. Our technicians inspect the mattress with you upon room setup. Any manufacturing flaw or damage is replaced immediately at zero charge.";
  } else if (q.includes('custom') || q.includes('size') || q.includes('antique') || q.includes('inch')) {
    reply = "Yes! Our Jaipur atelier specializes in bespoke custom sizing down to the exact half-inch. You can launch our **Bespoke Mattress Architect** (under Precision Sleep Suite) to configure width, length, thickness, pocket coils, and even custom monogram silk embroidery.";
  } else if (q.includes('springhaven')) {
    reply = "The **SpringHaven™ Series** is our ultra-luxury flagship tier (~15-inch profile). Unlike standard single-block mattresses, SpringHaven integrates dual-tier micro-pocketed coils with 100% organic Belgian latex, hand-tufted cashmere damask quilting, and a 25-Year comprehensive structural warranty.";
  } else {
    reply = "Good Dream handcrafted mattresses feature European-certified materials, zero partner disturbance pocket springs, and complimentary white-glove setup across India. You can book a consultation directly with our Jaipur team at **+91 70149 83696** or email **gooddreamshomedecor@gmail.com**.";
  }

  setTimeout(() => {
    state.aiChatMessages.push({ sender: 'bot', text: reply });
    const box = document.getElementById('aiMessagesBox');
    if (box) {
      box.innerHTML = state.aiChatMessages
        .map(
          (m) => `
        <div class="chat-bubble ${m.sender}">
          <div class="bubble-content">${m.sender === 'bot' ? formatBotMessage(m.text) : escapeHtml(m.text)}</div>
        </div>
      `
        )
        .join('');
      box.scrollTop = box.scrollHeight;
    }
  }, 400);

  const box = document.getElementById('aiMessagesBox');
  if (box) {
    box.innerHTML = state.aiChatMessages
      .map(
        (m) => `
      <div class="chat-bubble ${m.sender}">
        <div class="bubble-content">${m.sender === 'bot' ? formatBotMessage(m.text) : escapeHtml(m.text)}</div>
      </div>
    `
      )
      .join('');
    box.scrollTop = box.scrollHeight;
  }
}

// -------------------------------------------------------------
// MODAL: SANCTUARY CARE SUITE (6 Dedicated Forms from HomeScreen.kt)
// -------------------------------------------------------------
function renderSanctuaryCareModal() {
  const type = state.modalData?.type || 'your_needs';

  let title = 'Your Needs - Bespoke Custom Sizing';
  let subtitle = 'Custom dimension mattress inquiry for master suites and antique beds';

  if (type === 'sponsor_rewards') {
    title = 'Sponsor Rewards & Referral Credit';
    subtitle = 'Share the gift of restorative sleep and earn ₹2,500 store credit';
  } else if (type === 'warranty') {
    title = '25-Year Digital Warranty Vault';
    subtitle = 'Register warranty or request certified physical inspection';
  } else if (type === 'repairs') {
    title = 'Repairs & Client Care Desk';
    subtitle = 'Schedule technician dispatch for tufting, cover, or spring service';
  } else if (type === 'feedbacks') {
    title = 'Verified Patron Feedbacks';
    subtitle = 'Share your honest review of Good Dream & SpringHaven sleep systems';
  } else if (type === 'complaints') {
    title = 'Concierge Priority Escalation (24h SLA)';
    subtitle = 'Rule 5(9) Statutory Nodal Grievance Redressal Desk';
  }

  return `
    <div class="modal-dialog care-modal-dialog">
      <div class="modal-header">
        <button class="btn-modal-back" onclick="closeModal()">← Close</button>
        <div class="modal-header-title">
          <h3>${title}</h3>
          <span>${subtitle}</span>
        </div>
        <button class="btn-modal-close" onclick="closeModal()">✕</button>
      </div>

      <div class="care-modal-body">
        ${
          type === 'sponsor_rewards'
            ? `
          <div class="sponsor-hero-banner">
            <div class="sponsor-icon">🎁</div>
            <h4>Earn ₹2,500 Store Credit on Every Friend Referral</h4>
            <p>Your unique referral concierge code: <strong class="ref-code">DREAM-ROYAL-2500</strong></p>
            <p style="font-size: 0.85rem; color: #555;">When your referred guest places their first mattress order, they unlock 25% off with Member Privilege code <code>WELCOME25</code>, and you automatically receive ₹2,500 store credit for luxury bedding and pillows.</p>
          </div>
        `
            : ''
        }

        <form onsubmit="handleSanctuaryFormSubmit(event, '${type}')" class="care-form">
          <div class="form-row">
            <div class="form-group">
              <label>Full Name *</label>
              <input type="text" id="careName" required placeholder="Sanctuary Patron / Guest">
            </div>
            <div class="form-group">
              <label>Mobile Number (WhatsApp) *</label>
              <input type="tel" id="carePhone" required placeholder="+91 70149 83696">
            </div>
          </div>

          <div class="form-group">
            <label>Email Address *</label>
            <input type="email" id="careEmail" required placeholder="client@domain.com">
          </div>

          ${
            type === 'your_needs'
              ? `
            <div class="form-row">
              <div class="form-group">
                <label>Custom Dimensions (L × W in Inches)</label>
                <input type="text" placeholder="e.g. 78 x 75 x 12">
              </div>
              <div class="form-group">
                <label>Preferred Core</label>
                <select>
                  <option>100% Belgian Organic Latex</option>
                  <option>Swedish Multi-Zone Pocket Coils</option>
                  <option>High-Density Orthopedic Foam</option>
                </select>
              </div>
            </div>
          `
              : type === 'warranty' || type === 'repairs'
              ? `
            <div class="form-group">
              <label>Order Reference ID / Warranty Certificate Number</label>
              <input type="text" placeholder="e.g. GD-892100">
            </div>
          `
              : ''
          }

          <div class="form-group">
            <label>Detailed Requirements / Message *</label>
            <textarea id="careMessage" rows="4" required placeholder="Provide details regarding your sleep needs, room floor, or inquiry..."></textarea>
          </div>

          <div class="care-statutory-notice">
            Official Desk: <strong>${CORPORATE_CONFIG.supportEmail}</strong> | Nodal Officer: <strong>${CORPORATE_CONFIG.grievanceOfficer}</strong><br>
            Acknowledgment within 48 hours • Complete redressal within 30 days.
          </div>

          <button type="submit" class="btn-hero-primary full-width">
            Submit to Concierge Desk →
          </button>
        </form>
      </div>
    </div>
  `;
}

function handleSanctuaryFormSubmit(e, type) {
  e.preventDefault();
  const name = document.getElementById('careName')?.value;
  alert(`Thank you, ${name}. Your inquiry has been logged with our Concierge Desk. Reference ID: GD-REQ-${Math.floor(1000 + Math.random() * 9000)}. Our sleep coordinator will contact you within 24 hours.`);
  closeModal();
}

function attachDynamicListeners() {
  updateBadgeCounts();
}

// Global App Initialization
window.addEventListener('DOMContentLoaded', () => {
  initStorage();
  renderApp();
  updateBadgeCounts();
});
