import React, { useState, useEffect, useMemo, useRef } from 'react';
import { 
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer 
} from 'recharts';
import { 
  ArrowTrendingUpIcon, MapIcon, HomeIcon, TableCellsIcon, 
  MagnifyingGlassIcon, ChevronUpDownIcon, FunnelIcon, XMarkIcon, 
  ArrowLeftIcon, UserCircleIcon, StarIcon, BuildingOffice2Icon, KeyIcon,
  SparklesIcon,
  ChatBubbleLeftRightIcon,
  ArrowRightOnRectangleIcon
} from '@heroicons/react/24/solid';
import { StarIcon as StarIconOutline } from '@heroicons/react/24/outline';

import { ComposableMap, Geographies, Geography, ZoomableGroup } from "react-simple-maps";
import { scaleLinear, scaleQuantile } from "d3-scale";
import { Tooltip as ReactTooltip } from "react-tooltip";

/**
 * ==============================================================================
 * [PART 1] 기존 레거시 로직 유지 (CSV 파서 & 히트맵)
 * ==============================================================================
 */
const parseCSV = (text) => {
  const lines = text.trim().split('\n');
  if (lines.length < 4) return [];

  const splitCSV = (row) => row.match(/(".*?"|[^",]+)(?=\s*,|\s*$)/g) || [];
  const headerRow = splitCSV(lines[0]);
  const dateHeaders = headerRow.slice(5).map(d => d.replace(/^[\ufeff"]+|["\r]+$/g, '').trim());

  const parsedData = [];

  for (let i = 3; i < lines.length; i++) {
    const row = splitCSV(lines[i]);
    if (row.length < 5) continue;

    const regions = row.slice(1, 5).map(r => r.replace(/["\r]/g, '').trim());
    const uniqueRegions = [...new Set(regions)].filter(r => r && r !== '지역');
    const regionName = uniqueRegions.join(' ');

    const historyData = row.slice(5).map((priceStr, index) => {
      const raw = priceStr ? parseFloat(priceStr.replace(/[",\r]/g, '')) : 0;
      const price = Math.round(raw * 3.3);
      const dateRaw = dateHeaders[index] || "";

      // 날짜 파싱
      const nums = dateRaw.match(/\d+/g);
      let year = 0, month = 0;
      if (nums && nums.length >= 2) {
          year = parseInt(nums[0]);
          if (year < 100) year += 2000;
          month = parseInt(nums[1]);
      }

      // [핵심 수정] 
      // 기존: (year === 2025 && month >= 6) 조건 때문에 9,10,11월이 죽었음.
      // 수정: 오직 '예측' 텍스트가 있거나, 명백한 미래(25년 12월 이상)인 경우만 예측으로 간주
      const isPredicted = dateRaw.includes('예측') || (year === 2025 && month >= 12) || (year > 2025);

      return {
        displayDate: `${year}.${String(month).padStart(2, '0')}`,
        year, month, price, isPredicted
      };
    });

    // 0원 제외 및 최신 데이터 찾기
    const validData = historyData.filter(d => d.price > 0 && !d.isPredicted);
    
    // 현재가: 11월 데이터가 있으면 쓰고, 없으면 유효한 마지막 데이터 사용
    const currentItem = historyData.find(d => d.year === 2025 && d.month === 11) || validData[validData.length - 1];
    const currentPrice = currentItem ? currentItem.price : 0;
    
    // 차트의 마지막(미래 포함)
    const futurePrice = historyData[historyData.length - 1].price; 

    // 전월/전년
    const prevMonthPrice = historyData.find(d => d.year === 2025 && d.month === 10)?.price || 0;
    const prevYearPrice = historyData.find(d => d.year === 2024 && d.month === 11)?.price || 0;

    if (currentPrice === 0) continue;

    parsedData.push({
      id: i,
      region: regionName,
      history: historyData,
      currentPrice, futurePrice, prevMonthPrice, prevYearPrice
    });
  }
  return parsedData;
};

const HeatmapView = ({ data, mode = 'simple' }) => {
  const [tooltipContent, setTooltipContent] = useState("");
  const [mapMode, setMapMode] = useState('price'); 
  const [viewState, setViewState] = useState({ 
    scale: 4000, 
    center: [127.6, 35.9], 
    region: 'South Korea'
  });

  const GEO_URL_KOREA = "https://raw.githubusercontent.com/southkorea/southkorea-maps/master/kostat/2013/json/skorea_provinces_topo_simple.json";
  const GEO_URL_MUNI = "https://raw.githubusercontent.com/southkorea/southkorea-maps/master/kostat/2013/json/skorea_municipalities_topo_simple.json";

  const REGION_CONFIG = {
    'South Korea': { scale: 4000, center: [127.6, 35.9] },
    'Seoul': { scale: 45000, center: [126.98, 37.56] },
    'Busan': { scale: 45000, center: [129.07, 35.18] },
    'Daegu': { scale: 35000, center: [128.60, 35.87] },
    'Incheon': { scale: 30000, center: [126.70, 37.45] },
    'Gwangju': { scale: 45000, center: [126.85, 35.16] },
    'Daejeon': { scale: 45000, center: [127.38, 36.35] },
    'Ulsan': { scale: 35000, center: [129.31, 35.53] },
    'Sejong': { scale: 60000, center: [127.28, 36.48] },
    'Gyeonggi': { scale: 12000, center: [127.20, 37.40] },
    'Gangwon': { scale: 9000, center: [128.30, 37.80] },
    'Chungbuk': { scale: 12000, center: [127.70, 36.80] },
    'Chungnam': { scale: 12000, center: [126.80, 36.50] },
    'Jeonbuk': { scale: 12000, center: [127.10, 35.80] },
    'Jeonnam': { scale: 10000, center: [126.90, 34.80] },
    'Gyeongbuk': { scale: 9000, center: [128.70, 36.40] },
    'Gyeongnam': { scale: 11000, center: [128.20, 35.40] },
    'Jeju': { scale: 25000, center: [126.50, 33.38] },
  };

  const normalizeName = (geoName) => {
    if (!geoName) return "";
    const name = geoName.trim();
    if (["서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"].includes(name)) return name;
    if (name.includes("서울")) return "서울";
    if (name.includes("부산")) return "부산";
    if (name.includes("대구")) return "대구";
    if (name.includes("인천")) return "인천";
    if (name.includes("광주")) return "광주";
    if (name.includes("대전")) return "대전";
    if (name.includes("울산")) return "울산";
    if (name.includes("세종")) return "세종";
    if (name.includes("경기")) return "경기";
    if (name.includes("강원")) return "강원";
    if (name.includes("충북") || name.includes("충청북도")) return "충북";
    if (name.includes("충남") || name.includes("충청남도")) return "충남";
    if (name.includes("전북") || name.includes("전라북도")) return "전북";
    if (name.includes("전남") || name.includes("전라남도")) return "전남";
    if (name.includes("경북") || name.includes("경상북도")) return "경북";
    if (name.includes("경남") || name.includes("경상남도")) return "경남";
    if (name.includes("제주")) return "제주";
    return "";
  };

  const legendStats = useMemo(() => {
    const validData = data.filter(d => d.region !== '전국' && d.currentPrice > 0);
    if (validData.length === 0) return { minPrice: 0, maxPrice: 0 };
    const prices = validData.map(d => d.currentPrice);
    return { minPrice: Math.min(...prices), maxPrice: Math.max(...prices) };
  }, [data]);

  const colorScale = useMemo(() => {
    let validData = data.filter(d => d.region !== '전국' && d.currentPrice > 0);
    if (viewState.region !== 'South Korea') {
        const localName = normalizeName(viewState.region); 
        validData = data.filter(d => d.region.startsWith(localName));
    }

    if (validData.length === 0) return () => "#F3F4F6";

    if (mapMode === 'price') {
      const prices = validData.map(d => d.currentPrice);
      return scaleQuantile()
        .domain(prices)
        .range(["#3b82f6", "#60a5fa", "#22c55e", "#84cc16", "#eab308", "#f97316", "#ef4444"]);
    } else {
      const growths = validData.map(d => {
        const base = mapMode === 'growth_mom' ? d.prevMonthPrice : d.prevYearPrice;
        if (!base) return 0;
        return ((d.currentPrice - base) / base) * 100;
      });
      let maxAbs = Math.max(...growths.map(Math.abs)) || 1;
      const limit = mapMode === 'growth_mom' ? 1.5 : 5.0; 
      maxAbs = Math.min(maxAbs, limit); 
      return scaleLinear()
        .domain([-maxAbs, 0, maxAbs]) 
        .range(["#1e3a8a", "#ffffff", "#b91c1c"])
        .clamp(true);
    }
  }, [data, mapMode, viewState.region]);

  const getLegendLabels = () => {
    if (mapMode === 'price') {
      const min = (legendStats.minPrice / 10000).toFixed(0);
      const max = (legendStats.maxPrice / 10000).toFixed(0);
      return { left: `${min}만원`, right: `${max}만원`, center: '평균' };
    } else {
      const limit = mapMode === 'growth_mom' ? 1.5 : 5.0;
      return { left: `-${limit}% ▼`, right: `+${limit}% ▲`, center: '0%' };
    }
  };
  const labels = getLegendLabels();

  const handleGeographyClick = (geo) => {
    if (mode === 'simple') return;
    const geoName = geo.properties.name || geo.properties.name_eng;
    if (viewState.region === 'South Korea') {
        let targetRegion = null;
        Object.keys(REGION_CONFIG).forEach(key => {
            if (geoName.includes(key) || (key === 'Seoul' && geoName.includes('서울'))) {
                targetRegion = key;
            }
        });
        if (targetRegion && REGION_CONFIG[targetRegion]) {
            setViewState({ region: targetRegion, ...REGION_CONFIG[targetRegion] });
        }
    }
  };

  const handleBackToNational = () => {
    setViewState({ region: 'South Korea', ...REGION_CONFIG['South Korea'] });
  };

  return (
    <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100/50 h-full flex flex-col relative overflow-hidden">
      <div className="flex justify-between items-start z-10 relative">
        <div className='flex flex-col items-start'>
          <div className='flex items-center gap-2'>
            {viewState.region !== 'South Korea' && (
                <button onClick={handleBackToNational} className="p-1 rounded-full bg-gray-100 hover:bg-gray-200 transition-colors">
                    <ArrowLeftIcon className="w-4 h-4 text-gray-600" />
                </button>
            )}
            <h2 className="text-xl font-bold text-gray-900">
                {viewState.region === 'South Korea' ? (mapMode === 'price' ? '전국 시세 히트맵' : '전국 상승률 히트맵') : `${normalizeName(viewState.region)} 상세 지도`}
            </h2>
          </div>
          <p className="text-sm text-gray-500 mb-4 mt-1">
            {mapMode === 'price' ? '25년 11월 기준 평당가' : '변동폭 기준'}
          </p>
          <div className="bg-white/80 backdrop-blur-sm p-3 rounded-xl border border-gray-200 shadow-sm inline-block min-w-[200px]">
            <div className="flex justify-between text-[10px] text-gray-500 mb-1 font-bold">
              <span>{labels.left}</span>
              <span className="text-gray-400 font-normal">{labels.center}</span>
              <span>{labels.right}</span>
            </div>
            <div className={`h-2 rounded-full w-full ${mapMode === 'price' ? 'bg-gradient-to-r from-blue-500 via-green-400 via-yellow-400 to-red-500' : 'bg-gradient-to-r from-blue-900 via-white to-red-700 border border-gray-200'}`}></div>
          </div>
        </div>
        <div className="bg-gray-100 p-1 rounded-xl flex gap-1">
          {['price', 'growth_mom', 'growth_yoy'].map((m) => (
             <button key={m} onClick={() => setMapMode(m)} className={`px-3 py-2 text-xs font-bold rounded-lg transition-all ${mapMode === m ? (m === 'price' ? 'bg-white text-blue-600 shadow-sm' : 'bg-white text-red-600 shadow-sm') : 'text-gray-500 hover:text-gray-700'}`}>
                {m === 'price' ? '평당가' : (m === 'growth_mom' ? '전월대비' : '전년대비')}
             </button>
          ))}
        </div>
      </div>

      <div className="flex-1 w-full h-full min-h-[400px] flex items-center justify-center -mt-10">
        <ComposableMap projection="geoMercator" projectionConfig={{ scale: viewState.scale, center: viewState.center }} style={{ width: "100%", height: "100%" }}>
            <ZoomableGroup center={viewState.center} zoom={1} minZoom={1} maxZoom={1} filterZoomEvent={() => false} translateExtent={[[0, 0], [800, 600]]} 
               // moveTransitionDuration={500}  <-- 경고 해결: 삭제하거나 prop 이름 확인 필요 (react-simple-maps 버전에 따라 다름)
            >
                <Geographies geography={viewState.region === 'South Korea' ? GEO_URL_KOREA : GEO_URL_MUNI}>
                    {({ geographies }) =>
                    geographies.map((geo) => {
                        const geoName = geo.properties.name || geo.properties.name_eng;
                        let kName = "";
                        if (viewState.region === 'South Korea') {
                            kName = normalizeName(geoName);
                        } else {
                            const parentName = normalizeName(viewState.region);
                            kName = `${parentName} ${geoName}`;
                        }
                        
                        let matchedItems = [];
                        if (viewState.region === 'South Korea') {
                            matchedItems = data.filter(d => d.region === kName);
                            if (matchedItems.length === 0 && kName) {
                                matchedItems = data.filter(d => d.region.startsWith(kName));
                            }
                        } else {
                            matchedItems = data.filter(d => d.region === kName || d.region.includes(kName));
                        }

                        let value = 0;
                        let displayValue = "";
                        
                        if (matchedItems.length > 0) {
                            const avgPrice = matchedItems.reduce((acc, curr) => acc + curr.currentPrice, 0) / matchedItems.length;
                            const avgPrevMonth = matchedItems.reduce((acc, curr) => acc + curr.prevMonthPrice, 0) / matchedItems.length;
                            
                            if (mapMode === 'price') {
                                value = avgPrice;
                                displayValue = `${(value / 10000).toFixed(0)}만원`;
                            } else {
                                const base = avgPrevMonth;
                                value = base ? ((avgPrice - base) / base) * 100 : 0;
                                displayValue = `${value > 0 ? '+' : ''}${value.toFixed(2)}%`;
                            }
                        }

                        return (
                        <Geography
                            key={geo.rsmKey}
                            geography={geo}
                            fill={matchedItems.length > 0 ? colorScale(value) : "#F3F4F6"}
                            stroke="#FFFFFF"
                            strokeWidth={viewState.region === 'South Korea' ? 0.8 : 0.3}
                            style={{
                                default: { outline: "none", transition: "all 300ms" },
                                hover: { 
                                    fill: mode === 'interactive' && viewState.region === 'South Korea' ? "#1e293b" : (matchedItems.length > 0 ? colorScale(value) : "#F3F4F6"), 
                                    outline: "none", 
                                    cursor: mode === 'interactive' && viewState.region === 'South Korea' ? "pointer" : "default",
                                    stroke: "#1e293b", strokeWidth: 1.5 
                                },
                                pressed: { outline: "none" },
                            }}
                            onClick={() => handleGeographyClick(geo)}
                            onMouseEnter={() => {
                                const label = kName || geoName;
                                const text = matchedItems.length > 0 ? displayValue : "데이터 없음";
                                setTooltipContent(`${label}: ${text}`);
                            }}
                            onMouseLeave={() => setTooltipContent("")}
                            data-tooltip-id="map-tooltip"
                            data-tooltip-content={tooltipContent}
                        />
                        );
                    })
                    }
                </Geographies>
            </ZoomableGroup>
        </ComposableMap>
        <ReactTooltip id="map-tooltip" place="top" variant="dark" style={{ fontSize: '13px', fontWeight: 'bold', zIndex: 100 }} />
      </div>
    </div>
  );
};


/**
 * ==============================================================================
 * [PART 2] 신규 기능 추가 (API 서비스 & 컴포넌트)
 * ==============================================================================
 */

// 기존 API_BASE_URL (Java용) 아래에 Python용 주소 추가
const API_BASE_URL = "http://localhost:8080/api";
const PYTHON_API_URL = "http://localhost:8000/api"; // [추가] RAG 서버 주소

const api = {
  // 1. 로그인
  login: async (email, password) => {
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    if (!response.ok) throw new Error("로그인 실패");
    return response.json();
  },

  // 2. 회원가입 (수정됨: nickname -> name)
  signup: async (email, password, nickname) => {
    const response = await fetch(`${API_BASE_URL}/auth/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      // ★ 중요: 백엔드 DB 컬럼명 'name'에 맞춰서 전송
      body: JSON.stringify({ email, password, name: nickname, role: "USER" }) 
    });
    if (!response.ok) throw new Error("회원가입 실패");
    return response.json();
  },

  // 3. 실거래 매물 검색 (수정됨: minLat 에러 방지용 기본값 추가)
  searchMapItems: async (keyword) => {
    const params = new URLSearchParams();
    if (keyword) params.append("q", keyword);
    // 지도 범위 필수값 채우기
    params.append("minLat", "33");
    params.append("maxLat", "39");
    params.append("minLon", "124");
    params.append("maxLon", "132");

    const response = await fetch(`${API_BASE_URL}/map/items?${params.toString()}`);
    if (!response.ok) throw new Error("검색 실패");
    return response.json(); 
  },

  // 4. 거래 내역 상세 조회
  getTransactions: async (itemId) => {
    const response = await fetch(`${API_BASE_URL}/items/${itemId}/transactions`);
    if (!response.ok) throw new Error("거래 내역 조회 실패");
    return response.json();
  },

  // 5. 즐겨찾기 목록
  getFavorites: async (token) => {
    const response = await fetch(`${API_BASE_URL}/favorites`, {
      method: 'GET',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (!response.ok) throw new Error("즐겨찾기 조회 실패");
    return response.json();
  },

  // [추가] RAG 서버와 통신하는 함수
  analyzeRegion: async (region, query) => {
    const response = await fetch(`${PYTHON_API_URL}/rag/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ region, query })
    });
    if (!response.ok) throw new Error("AI 분석 실패 (Python 서버 확인 필요)");
    return response.json();
  }
};

// [수정] 로그인/회원가입 모달
const AuthModal = ({ isOpen, onClose, onLogin }) => {
  const [isSignup, setIsSignup] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nickname, setNickname] = useState(''); // 닉네임 상태 추가

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (isSignup) {
        await api.signup(email, password, nickname);
        alert("회원가입 성공! 로그인해주세요.");
        setIsSignup(false);
      } else {
        const data = await api.login(email, password);
        onLogin(data.user, data.accessToken);
        onClose();
      }
    } catch (err) {
      alert(isSignup ? "회원가입 실패" : "로그인 실패 (아이디/비번을 확인하세요)");
      console.error(err);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose}></div>
      <div className="relative w-full max-w-md bg-white/90 backdrop-blur-xl rounded-3xl shadow-2xl p-8 border border-white/50">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-blue-600 rounded-2xl mx-auto flex items-center justify-center shadow-lg shadow-blue-500/30 mb-4">
            <UserCircleIcon className="w-10 h-10 text-white" />
          </div>
          <h2 className="text-2xl font-bold text-gray-900">{isSignup ? '회원가입' : '환영합니다!'}</h2>
          <p className="text-gray-500 text-sm mt-2">{isSignup ? '서비스 이용을 위해 계정을 생성하세요' : 'SSAFY HOME 서비스를 이용하시려면 로그인하세요'}</p>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-bold text-gray-500 uppercase mb-1 ml-1">이메일</label>
            <input type="email" required autoComplete="username" className="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-blue-500 transition-all" placeholder="example@ssafy.com" value={email} onChange={e => setEmail(e.target.value)} />
          </div>
          <div>
            <label className="block text-xs font-bold text-gray-500 uppercase mb-1 ml-1">비밀번호</label>
            <input type="password" required autoComplete="current-password" className="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-blue-500 transition-all" placeholder="••••••••" value={password} onChange={e => setPassword(e.target.value)} />
          </div>
          {isSignup && (
             <div>
               <label className="block text-xs font-bold text-gray-500 uppercase mb-1 ml-1">이름</label>
               <input type="text" required autoComplete="nickname" className="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 outline-none focus:ring-2 focus:ring-blue-500 transition-all" placeholder="사용자 이름" value={nickname} onChange={e => setNickname(e.target.value)} />
             </div>
          )}
          <button type="submit" className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 rounded-xl shadow-lg shadow-blue-500/30 transition-all transform active:scale-95 mt-4">
            {isSignup ? '가입하기' : '로그인'}
          </button>
        </form>
        <div className="mt-6 text-center text-sm text-gray-500">
          <button onClick={() => setIsSignup(!isSignup)} className="text-blue-600 font-bold hover:underline">
            {isSignup ? '로그인으로 돌아가기' : '회원가입 하기'}
          </button>
        </div>
      </div>
    </div>
  );
};

const RealTradeTab = ({ user, onOpenLogin }) => {
  const [keyword, setKeyword] = useState('');
  const [items, setItems] = useState([]);
  const [selectedItem, setSelectedItem] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
        const res = await api.searchMapItems(keyword);
        setItems(res.items || []);
    } catch (e) {
        console.error(e);
        alert("데이터 조회 중 오류가 발생했습니다.");
    } finally {
        setLoading(false);
    }
    setSelectedItem(null); 
  };

  const handleSelect = async (item) => {
    setSelectedItem(item);
    try {
        const res = await api.getTransactions(item.id);
        setTransactions(res.items || []);
    } catch(e) {
        console.error(e);
        setTransactions([]);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 h-[calc(100vh-140px)]">
      <div className="lg:col-span-4 bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex flex-col h-full">
        <h2 className="text-xl font-bold mb-4 flex items-center gap-2">
          <BuildingOffice2Icon className="w-6 h-6 text-blue-500" />
          실거래 매물 찾기
        </h2>
        <form onSubmit={handleSearch} className="relative mb-6">
          <input type="text" placeholder="아파트명, 지역명 검색 (API)" className="w-full bg-gray-50 border-0 rounded-2xl pl-12 pr-4 py-3.5 text-sm shadow-inner focus:ring-2 focus:ring-blue-500 transition-all outline-none" value={keyword} onChange={e => setKeyword(e.target.value)} />
          <MagnifyingGlassIcon className="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2" />
        </form>
        <div className="flex-1 overflow-y-auto space-y-3 pr-2 custom-scrollbar">
          {loading ? (
            <div className="text-center py-10 text-gray-400">검색 중...</div>
          ) : items.length === 0 ? (
            <div className="text-center py-10 text-gray-400 text-sm">검색 결과가 없습니다.<br/>(예: 반포자이)</div>
          ) : (
            items.map(item => (
              <div key={item.id} onClick={() => handleSelect(item)} className={`p-4 rounded-2xl border transition-all cursor-pointer hover:shadow-md ${selectedItem?.id === item.id ? 'border-blue-500 bg-blue-50/50 ring-1 ring-blue-500' : 'border-gray-100 bg-white hover:border-blue-200'}`}>
                <div className="flex justify-between items-start mb-1">
                  <h3 className="font-bold text-gray-900">{item.name}</h3>
                  {item.type && <span className="bg-gray-100 text-gray-500 text-[10px] px-2 py-0.5 rounded-full font-bold">{item.type}</span>}
                </div>
                <p className="text-xs text-gray-500 mb-2">{item.address}</p>
                {item.latestPrice && <p className="text-sm font-bold text-blue-600">최근 {item.latestPrice.toLocaleString()}원</p>}
              </div>
            ))
          )}
        </div>
      </div>
      <div className="lg:col-span-8 flex flex-col gap-6 h-full overflow-hidden">
        {selectedItem ? (
          <>
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex justify-between items-center">
              <div>
                <div className="flex items-center gap-3 mb-1">
                  <h2 className="text-2xl font-bold text-gray-900">{selectedItem.name}</h2>
                  <span className="text-sm text-gray-400 font-medium">{selectedItem.builtYear}년 준공</span>
                </div>
                <p className="text-gray-500">{selectedItem.address}</p>
              </div>
              <div className="flex gap-3">
                <button onClick={() => user ? alert("찜 완료!") : onOpenLogin()} className="flex items-center gap-2 px-4 py-2 bg-yellow-50 text-yellow-600 rounded-xl font-bold hover:bg-yellow-100 transition-colors">
                  <StarIconOutline className="w-5 h-5" /> 찜하기
                </button>
              </div>
            </div>
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 flex-1 flex flex-col overflow-hidden">
              <h3 className="font-bold text-gray-800 mb-4">실거래 상세 내역</h3>
              <div className="flex-1 overflow-y-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-gray-50 sticky top-0 text-xs font-semibold text-gray-500 uppercase">
                    <tr><th className="px-4 py-3">계약일</th><th className="px-4 py-3">유형</th><th className="px-4 py-3">면적</th><th className="px-4 py-3">층</th><th className="px-4 py-3 text-right">금액</th></tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50">
                    {transactions.length === 0 ? (
                        <tr><td colSpan="5" className="text-center py-10 text-gray-400">거래 내역이 없습니다.</td></tr>
                    ) : (
                        transactions.map((t, idx) => (
                        <tr key={idx} className="hover:bg-gray-50"><td className="px-4 py-3">{t.contractDate}</td><td className="px-4 py-3">{t.type}</td><td className="px-4 py-3">{t.area}㎡</td><td className="px-4 py-3">{t.floor}층</td><td className="px-4 py-3 text-right font-bold">{t.price.toLocaleString()}원</td></tr>
                        ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        ) : (
          <div className="h-full bg-white rounded-3xl border border-gray-100 border-dashed flex flex-col items-center justify-center text-gray-400 gap-4">
            <BuildingOffice2Icon className="w-16 h-16 text-gray-200" />
            <p>좌측에서 단지를 선택하세요.</p>
          </div>
        )}
      </div>
    </div>
  );
};

const FavoritesTab = ({ user, onOpenLogin }) => {
  const [favorites, setFavorites] = useState([]);
  
  useEffect(() => {
    if (user) {
        api.getFavorites(user.token)
           .then(res => setFavorites(res.items || []))
           .catch(err => console.error(err));
    }
  }, [user]);

  if (!user) {
    return (
      <div className="h-[500px] flex flex-col items-center justify-center bg-white rounded-3xl shadow-sm border border-gray-100">
        <KeyIcon className="w-20 h-20 text-gray-200 mb-6" />
        <h2 className="text-xl font-bold text-gray-900 mb-2">로그인이 필요한 서비스입니다</h2>
        <button onClick={onOpenLogin} className="px-8 py-3 bg-blue-600 text-white font-bold rounded-xl shadow-lg hover:bg-blue-700 transition-all">로그인 하러가기</button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h2 className="text-2xl font-bold text-gray-900 flex items-center gap-2"><StarIcon className="w-7 h-7 text-yellow-400" />나의 관심 단지</h2>
      {favorites.length === 0 ? (
          <div className="text-gray-400 py-10 text-center">즐겨찾기한 단지가 없습니다.</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {favorites.map(item => (
            <div key={item.id} className="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 hover:shadow-md transition-all relative">
                <StarIcon className="w-6 h-6 text-yellow-400 absolute top-6 right-6" />
                <h3 className="text-xl font-bold text-gray-900 mb-1">{item.name}</h3>
                <p className="text-sm text-gray-500 mb-4">{item.address}</p>
                <p className="text-lg font-bold text-gray-900">{item.latestPrice ? item.latestPrice.toLocaleString() + '원' : '-'}</p>
            </div>
            ))}
        </div>
      )}
    </div>
  );
};

/**
 * ==============================================================================
 * [수정됨] 최근 3개월 평균 등락률 기반 예측 함수
 * ==============================================================================
 */
const calculateLinearForecast = (historyData) => {
  // 1. 유효 데이터 필터링 (0원 제외, 예측값 제외)
  const validHistory = historyData.filter(d => !d.isPredicted && d.price > 0);
  
  if (validHistory.length < 2) return { price: 0, rate: 0, isPositive: false };

  // 2. 날짜 오름차순 정렬 (필수: 과거 -> 현재)
  validHistory.sort((a, b) => {
      if (a.year !== b.year) return a.year - b.year;
      return a.month - b.month;
  });

  // 3. 최근 1년(12개월) 데이터 추출
  // 데이터가 12개보다 적으면 있는 것만 사용
  const recentData = validHistory.slice(-12);
  const n = recentData.length;

  // 4. 선형 회귀 계산 (Least Squares Method)
  // x: 0, 1, 2... (시간), y: 가격
  let sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
  
  recentData.forEach((d, i) => {
      sumX += i;
      sumY += d.price;
      sumXY += i * d.price;
      sumXX += i * i;
  });

  const slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
  const intercept = (sumY - slope * sumX) / n;

  // 5. 다음 달(n번째 인덱스) 예측
  const nextIndex = n; 
  let predictedPrice = Math.round(slope * nextIndex + intercept);

  // 6. 등락률 계산 (마지막 실제 가격 대비)
  const lastActualPrice = recentData[n - 1].price;
  const rate = ((predictedPrice - lastActualPrice) / lastActualPrice * 100);

  return {
      price: predictedPrice,
      rate: isNaN(rate) ? 0 : rate.toFixed(2),
      isPositive: rate > 0
  };
};
/**
 * ==============================================================================
 * [LegacyDashboard] 수정된 예측 로직 적용
 * ==============================================================================
 */
const LegacyDashboard = ({ data }) => {
  const [selectedRegion, setSelectedRegion] = useState(null);
  const [isRegionSearchOpen, setIsRegionSearchOpen] = useState(false);
  const [regionSearchQuery, setRegionSearchQuery] = useState('');
  const [viewMode, setViewMode] = useState('ALL');
  const [sortConfig, setSortConfig] = useState({ key: 'currentPrice', direction: 'desc' });
  const searchInputRef = useRef(null);

  // AI 상태
  const [aiAnalysis, setAiAnalysis] = useState("");
  const [isAiLoading, setIsAiLoading] = useState(false);

  useEffect(() => {
    if (data.length > 0 && !selectedRegion) {
      const defaultRegion = data.find(d => d.region === '전국') || data[0];
      setSelectedRegion(defaultRegion);
    }
  }, [data, selectedRegion]);

  useEffect(() => {
    if (!selectedRegion) return;
    const fetchAI = async () => {
      setIsAiLoading(true);
      setAiAnalysis(""); 
      try {
        const res = await api.analyzeRegion(selectedRegion.region, "향후 전망과 투자 가치를 요약해줘");
        setAiAnalysis(res.result);
      } catch (err) {
        setAiAnalysis("분석 서버 연결 실패 (Python 서버 확인 필요)");
      } finally {
        setIsAiLoading(false);
      }
    };
    fetchAI();
  }, [selectedRegion]);

  // 카드 표시용 예측 데이터
  const predictionInfo = useMemo(() => {
      if (!selectedRegion || !selectedRegion.history) return { price: 0, rate: 0, isPositive: false };
      return calculateLinearForecast(selectedRegion.history);
  }, [selectedRegion]);

  // 검색 제안
  const searchSuggestions = useMemo(() => {
      if (!regionSearchQuery) return [];
      return data.filter(item => item.region.includes(regionSearchQuery)).slice(0, 10);
  }, [data, regionSearchQuery]);

  const handleRegionSelect = (item) => {
      setSelectedRegion(item);
      setIsRegionSearchOpen(false);
      setRegionSearchQuery('');
  };

  const chartData = useMemo(() => {
      if (!selectedRegion || !selectedRegion.history) return [];
      return viewMode === 'ALL' ? selectedRegion.history : selectedRegion.history.filter(d => d.year === parseInt(viewMode));
  }, [selectedRegion, viewMode]);

  const yAxisDomain = useMemo(() => {
      if (chartData.length === 0) return [0, 'auto'];
      const prices = chartData.map(d => d.price);
      const min = Math.min(...prices);
      const max = Math.max(...prices);
      return [Math.max(0, min - (min * 0.1)), max + (max * 0.05)];
  }, [chartData]);

  const trendMoM = selectedRegion && selectedRegion.prevMonthPrice ? ((selectedRegion.currentPrice - selectedRegion.prevMonthPrice)/selectedRegion.prevMonthPrice * 100).toFixed(1) : 0;
  const trendYoY = selectedRegion && selectedRegion.prevYearPrice ? ((selectedRegion.currentPrice - selectedRegion.prevYearPrice)/selectedRegion.prevYearPrice * 100).toFixed(1) : 0;
  const availableYears = Array.from({ length: 2025 - 2012 + 1 }, (_, i) => 2012 + i);

  // [수정] 리스트 데이터 처리 (예측값 및 등락률 미리 계산)
  const processedData = useMemo(() => {
      let result = data.map(item => {
          const forecast = calculateLinearForecast(item.history);
          return {
              ...item,
              forecastPrice: forecast.price,
              forecastRate: parseFloat(forecast.rate), // 정렬을 위해 숫자로 변환
              isForecastPositive: forecast.isPositive
          };
      });

      if (sortConfig.key) {
        result.sort((a, b) => {
          let aValue = a[sortConfig.key];
          let bValue = b[sortConfig.key];
          if (aValue < bValue) return sortConfig.direction === 'asc' ? -1 : 1;
          if (aValue > bValue) return sortConfig.direction === 'asc' ? 1 : -1;
          return 0;
        });
      }
      return result.slice(0, 50); 
  }, [data, sortConfig]);

  const handleSort = (key) => {
      let dir = 'desc';
      if (sortConfig.key === key && sortConfig.direction === 'desc') dir = 'asc';
      setSortConfig({ key, direction: dir });
  };

  if (!selectedRegion) return <div className="p-10 text-center text-gray-400">데이터 로딩 중...</div>;

  return (
    <div className="space-y-6 pb-10">
      {/* 1. 상단 카드 영역 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        <div className="relative h-full">
          {!isRegionSearchOpen ? (
            <div onClick={() => setIsRegionSearchOpen(true)} className="h-full p-6 rounded-3xl shadow-sm border border-gray-100/50 bg-white flex flex-col justify-between cursor-pointer hover:border-blue-300 transition-all group">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-gray-500 text-xs font-bold uppercase tracking-wider">선택 지역</h3>
                  <MapIcon className="w-6 h-6 text-blue-500" />
                </div>
                <span className="text-2xl font-bold text-gray-900 tracking-tight flex items-center gap-2">
                  {selectedRegion.region}
                  <MagnifyingGlassIcon className="w-5 h-5 text-gray-300 group-hover:text-blue-500 transition-colors" />
                </span>
              </div>
              <p className="text-xs text-gray-400 font-medium mt-3">클릭하여 다른 지역 검색</p>
            </div>
          ) : (
            <div className="h-full p-6 rounded-3xl shadow-md border-2 border-blue-500 bg-white flex flex-col absolute inset-0 z-30">
              <div className="flex items-center gap-2 mb-2">
                <MagnifyingGlassIcon className="w-5 h-5 text-blue-500" />
                <input 
                  ref={searchInputRef}
                  type="text"
                  className="w-full outline-none text-lg font-bold text-gray-900 placeholder-gray-300"
                  placeholder="지역명 입력"
                  value={regionSearchQuery}
                  onChange={(e) => setRegionSearchQuery(e.target.value)}
                  onBlur={() => setTimeout(() => setIsRegionSearchOpen(false), 200)}
                  autoFocus
                />
                <button onClick={() => setIsRegionSearchOpen(false)}><XMarkIcon className="w-6 h-6 text-gray-400" /></button>
              </div>
              {regionSearchQuery && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden z-40 max-h-60 overflow-y-auto">
                  {searchSuggestions.map(item => (
                    <div key={item.id} className="px-5 py-3 hover:bg-blue-50 cursor-pointer text-sm font-medium text-gray-700 border-b border-gray-50 last:border-0" onMouseDown={(e) => e.preventDefault()} onClick={() => handleRegionSelect(item)}>
                      {item.region}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="p-6 rounded-3xl shadow-sm border border-gray-100/50 bg-white flex flex-col justify-between">
          <div>
            <h3 className="text-gray-500 text-xs font-bold uppercase tracking-wider mb-2">현재 평당가 (25.11)</h3>
            <span className="text-2xl font-bold text-gray-900 tracking-tight block mb-4">{selectedRegion.currentPrice.toLocaleString()}원</span>
            <div className="space-y-1">
              <TrendRow label="전월 대비" value={trendMoM} />
              <TrendRow label="전년 대비" value={trendYoY} />
            </div>
          </div>
        </div>

        {/* [수정됨] 예측가 카드 (색상 적용) */}
        <div className="p-6 rounded-3xl shadow-sm border border-gray-100/50 bg-gradient-to-br from-indigo-50 to-white flex flex-col justify-between">
           <div>
             <div className="flex justify-between items-center mb-2">
                <h3 className="text-gray-500 text-xs font-bold uppercase tracking-wider">다음 달 예측 (25.12)</h3>
                <span className={`text-xs font-bold flex items-center px-2 py-0.5 rounded-full ${predictionInfo.isPositive ? 'bg-red-50 text-red-500' : 'bg-blue-50 text-blue-500'}`}>
                    {predictionInfo.isPositive ? '+' : ''}{predictionInfo.rate}%
                    <ArrowTrendingUpIcon className={`w-3 h-3 ml-1 ${predictionInfo.isPositive ? '' : 'rotate-180'}`} />
                </span>
             </div>
             <span className="text-2xl font-bold text-gray-900 tracking-tight block mb-4">
                {predictionInfo.price.toLocaleString()}원
             </span>
             <p className="text-xs text-indigo-500 font-bold bg-indigo-100 inline-block px-2 py-1 rounded-full">
                최근 1년 선형 회귀 분석
             </p>
           </div>
        </div>
      </div>

      {/* 2. 차트 & AI */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 h-[420px]">
        {/* 차트 */}
        <div className="lg:col-span-3 bg-white rounded-3xl p-7 flex flex-col shadow-sm border border-gray-100/50">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold mb-1 text-gray-900">{selectedRegion?.region} 가격 추이</h2>
            <div className="flex items-center bg-gray-100 rounded-lg px-3 py-1.5 border border-gray-200">
              <FunnelIcon className="w-4 h-4 text-gray-500 mr-2" />
              <select value={viewMode} onChange={(e) => setViewMode(e.target.value)} className="bg-transparent text-sm font-bold text-gray-700 outline-none cursor-pointer">
                <option value="ALL">전체 기간</option>
                {availableYears.map(y => <option key={y} value={y}>{y}년</option>)}
              </select>
            </div>
          </div>
          <div className="flex-1 w-full h-[300px]"> 
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorPrice" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#3B82F6" stopOpacity={0.15}/>
                    <stop offset="95%" stopColor="#3B82F6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
                <XAxis dataKey="displayDate" axisLine={false} tickLine={false} tick={{fontSize: 11, fill: '#94a3b8'}} minTickGap={30} />
                <YAxis domain={yAxisDomain} axisLine={false} tickLine={false} tickFormatter={(v) => `${(v/10000).toFixed(0)}만`} tick={{fontSize: 11, fill: '#94a3b8'}} width={45} />
                <RechartsTooltip contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }} formatter={(value) => [`${value.toLocaleString()}원`, '평당가']} />
                <Area type="monotone" dataKey="price" stroke="#3B82F6" strokeWidth={3} fill="url(#colorPrice)" animationDuration={1000} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* AI */}
        <div className="lg:col-span-2 bg-white rounded-3xl shadow-sm border border-purple-100 flex flex-col relative overflow-hidden">
            <div className="p-5 border-b border-purple-50 bg-purple-50/30 flex justify-between items-center">
                <h3 className="font-bold text-gray-800 flex items-center gap-2">
                    <SparklesIcon className="w-5 h-5 text-purple-600" />
                    AI 지역 분석 리포트
                </h3>
                {isAiLoading && <span className="text-xs font-bold text-purple-600 bg-purple-100 px-2 py-1 rounded-full animate-pulse">분석 중...</span>}
            </div>
            <div className="flex-1 p-6 overflow-y-auto custom-scrollbar bg-gradient-to-b from-white to-purple-50/10">
                {isAiLoading ? (
                    <div className="space-y-4 animate-pulse">
                        <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                        <div className="h-4 bg-gray-200 rounded w-full"></div>
                        <div className="h-4 bg-gray-200 rounded w-5/6"></div>
                        <div className="h-32 bg-gray-100 rounded-xl mt-4"></div>
                    </div>
                ) : aiAnalysis ? (
                    <div className="prose prose-sm prose-purple max-w-none text-gray-700 leading-relaxed whitespace-pre-wrap">
                        {aiAnalysis}
                    </div>
                ) : (
                    <div className="h-full flex flex-col items-center justify-center text-gray-400 text-center">
                        <ChatBubbleLeftRightIcon className="w-12 h-12 mb-2 opacity-20" />
                        <p className="text-sm">지역을 선택하면<br/>2025 리포트 기반 분석이 표시됩니다.</p>
                    </div>
                )}
            </div>
        </div>
      </div>

      {/* 3. 하단 리스트 */}
      <div className="bg-white rounded-3xl flex flex-col shadow-sm border border-gray-100/50 overflow-hidden min-h-[320px]">
        <div className="p-5 pb-3 bg-white sticky top-0 z-10 border-b border-gray-50">
          <h3 className="font-bold text-gray-800">지역별 시세 목록 (Top 50)</h3>
        </div>
        <div className="flex-1 overflow-y-auto max-h-[400px]">
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 sticky top-0 z-10 text-xs font-semibold text-gray-500 uppercase tracking-wider">
              <tr>
                <th className="px-6 py-4 cursor-pointer" onClick={() => handleSort('region')}>지역 <ChevronUpDownIcon className="w-3 h-3 inline" /></th>
                <th className="px-6 py-4 text-right cursor-pointer" onClick={() => handleSort('currentPrice')}>현재가 <ChevronUpDownIcon className="w-3 h-3 inline" /></th>
                <th className="px-6 py-4 text-right cursor-pointer" onClick={() => handleSort('forecastPrice')}>다음달 예측가 <ChevronUpDownIcon className="w-3 h-3 inline" /></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {processedData.map((item) => (
                <tr key={item.id} onClick={() => handleRegionSelect(item)} className={`cursor-pointer transition-all ${selectedRegion?.id === item.id ? 'bg-purple-50/60' : 'hover:bg-gray-50'}`}>
                  <td className="px-6 py-4 font-medium whitespace-normal break-words flex items-center gap-2">
                    {item.region}
                    {selectedRegion?.id === item.id && <span className="w-2 h-2 rounded-full bg-purple-500 animate-pulse"></span>}
                  </td>
                  <td className="px-6 py-4 text-right font-bold">{item.currentPrice.toLocaleString()}</td>
                  
                  {/* [수정됨] 색상 로직 적용된 예측가 컬럼 */}
                  <td className={`px-6 py-4 text-right font-bold ${
                      item.isForecastPositive ? 'text-red-600' : (item.forecastPrice < item.currentPrice ? 'text-blue-600' : 'text-gray-500')
                  }`}>
                    {item.forecastPrice > 0 ? (
                        <>
                            {item.forecastPrice.toLocaleString()}원
                            <span className="text-xs ml-1 font-normal opacity-80">
                                ({item.forecastRate > 0 ? '+' : ''}{item.forecastRate}%)
                            </span>
                        </>
                    ) : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

/**
 * ==============================================================================
 * [추가] AI 분석 탭 컴포넌트
 * ==============================================================================
 */

const AIAnalysisTab = () => {
  const [region, setRegion] = useState('');
  const [result, setResult] = useState('');
  const [loading, setLoading] = useState(false);

  const handleAnalyze = async (e) => {
    e.preventDefault();
    if (!region) return;
    setLoading(true);
    setResult('');
    
    try {
      const data = await api.analyzeRegion(region, "");
      setResult(data.result);
    } catch (err) {
      setResult("오류 발생: Python(8000) 서버가 켜져있는지 확인해주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 h-full min-h-[600px]">
      {/* 입력 영역 */}
      <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 flex flex-col justify-center">
        <div className="mb-8">
          <div className="w-14 h-14 bg-gradient-to-br from-purple-500 to-indigo-600 rounded-2xl flex items-center justify-center shadow-lg shadow-purple-500/30 mb-6">
            <SparklesIcon className="w-8 h-8 text-white animate-pulse" />
          </div>
          <h2 className="text-3xl font-bold text-gray-900 mb-3">AI 부동산 비서</h2>
          <p className="text-gray-500">2025 부동산 보고서를 기반으로 지역 전망을 분석합니다.</p>
        </div>
        <form onSubmit={handleAnalyze} className="space-y-4">
          <input type="text" className="w-full bg-gray-50 border border-gray-200 rounded-2xl px-6 py-4 text-lg font-bold outline-none focus:ring-2 focus:ring-purple-500 transition-all" placeholder="예: 서울시 강남구" value={region} onChange={(e) => setRegion(e.target.value)} />
          <button disabled={loading} className="w-full bg-gray-900 text-white font-bold py-4 rounded-2xl shadow-xl hover:bg-gray-800 transition-all flex items-center justify-center gap-2 disabled:bg-gray-300">
            {loading ? "분석 중..." : "AI 분석 시작하기"}
          </button>
        </form>
      </div>
      {/* 결과 영역 */}
      <div className="bg-white rounded-3xl p-8 shadow-sm border border-gray-100 flex flex-col relative overflow-hidden">
        <h3 className="text-xl font-bold text-gray-900 mb-6 flex items-center gap-2"><ChatBubbleLeftRightIcon className="w-6 h-6 text-gray-400" />분석 결과</h3>
        <div className="flex-1 overflow-y-auto pr-2 whitespace-pre-wrap text-gray-700 leading-relaxed">
            {result || <div className="text-gray-300 text-center mt-20">지역을 입력하면 AI가 답변해줍니다.</div>}
        </div>
      </div>
    </div>
  );
};

/**
 * ==============================================================================
 * [Main] App Container (Layout 통합)
 * ==============================================================================
 */
export default function RealEstateDashboard() {
  const [csvData, setCsvData] = useState([]);
  const [user, setUser] = useState(null); 
  const [token, setToken] = useState(null);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [currentTab, setCurrentTab] = useState('dashboard');
  
  useEffect(() => {
    fetch('/data.csv')
      .then(res => res.arrayBuffer())
      .then(buffer => {
        const decoder = new TextDecoder('euc-kr');
        const parsed = parseCSV(decoder.decode(buffer));
        setCsvData(parsed);
      })
      .catch(console.error);
  }, []);

  const handleLogin = (userData, accessToken) => {
    setUser(userData);
    setToken(accessToken);
  };
  const handleLogout = () => {
    setUser(null);
    setToken(null);
    if(currentTab === 'favorites') setCurrentTab('dashboard');
  };

  return (
    <div className="flex h-screen bg-[#F8F9FD] font-sans text-gray-800 selection:bg-blue-100">
      <AuthModal isOpen={isLoginModalOpen} onClose={() => setIsLoginModalOpen(false)} onLogin={handleLogin} />

      {/* Sidebar: Navigation */}
      <aside className="w-72 bg-white px-6 py-8 hidden md:flex flex-col shadow-xl shadow-gray-200/50 z-20">
        <div className="flex items-center gap-3 mb-12 px-2">
          <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/30">
            <HomeIcon className="w-6 h-6 text-white" />
          </div>
          <span className="text-xl font-extrabold tracking-tight text-gray-900">SSAFY HOME</span>
        </div>
        <nav className="space-y-2 flex-1">
          <div className="px-2 mb-2 text-xs font-bold text-gray-400 uppercase tracking-wider">분석</div>
          <NavItem icon={HomeIcon} text="대시보드" active={currentTab === 'dashboard'} onClick={() => setCurrentTab('dashboard')} />
          <NavItem icon={MapIcon} text="전국 시세 지도" active={currentTab === 'map'} onClick={() => setCurrentTab('map')} />
          <NavItem icon={SparklesIcon} text="AI 부동산 비서" active={currentTab === 'ai'} onClick={() => setCurrentTab('ai')} />
          <div className="px-2 mb-2 mt-8 text-xs font-bold text-gray-400 uppercase tracking-wider">실거래 서비스</div>
          <NavItem icon={BuildingOffice2Icon} text="실거래 찾기" active={currentTab === 'realtrade'} onClick={() => setCurrentTab('realtrade')} />
          <NavItem icon={StarIcon} text="마이 홈 (즐겨찾기)" active={currentTab === 'favorites'} onClick={() => setCurrentTab('favorites')} />
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col h-full overflow-hidden relative">
        <header className="h-20 flex items-center justify-between px-10 shrink-0 bg-white/50 backdrop-blur-md sticky top-0 z-30">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              {currentTab === 'dashboard' && '부동산 시장 동향'}
              {currentTab === 'map' && '지역별 시세 분석'}
              {currentTab === 'ai' && 'AI 부동산 전망 분석'} {/* [추가] */}
              {currentTab === 'realtrade' && '실거래가 조회'}
              {currentTab === 'favorites' && '나의 관심 매물'}
            </h1>
            <p className="text-sm text-gray-500 font-medium mt-1">
                {currentTab === 'dashboard' ? '데이터 기반 분석' : 'API 서비스 연동'}
            </p>
          </div>
          
          {/* 우측 상단 로그인 영역 (흰 화면 수정: nickname -> name) */}
          <div>
            {user ? (
              <div className="flex items-center gap-4 bg-white px-4 py-2 rounded-2xl shadow-sm border border-gray-100">
                <div className="flex items-center gap-3">
                    <div className="w-8 h-8 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-sm font-bold">
                        {/* 닉네임이 없으면 이름(name)을 사용하도록 안전하게 처리 */}
                        {(user.name || user.nickname || "U")[0]}
                    </div>
                    <div className="text-right hidden sm:block">
                        <p className="text-sm font-bold text-gray-900 leading-none">{user.name || user.nickname}님</p>
                        <p className="text-[10px] text-gray-400 mt-0.5">{user.email}</p>
                    </div>
                </div>
                <div className="h-6 w-px bg-gray-200 mx-1"></div>
                <button onClick={handleLogout} className="text-gray-400 hover:text-red-500 transition-colors p-1" title="로그아웃">
                    <ArrowRightOnRectangleIcon className="w-5 h-5" />
                </button>
              </div>
            ) : (
              <button 
                onClick={() => setIsLoginModalOpen(true)}
                className="flex items-center gap-2 bg-gray-900 text-white font-bold py-2.5 px-6 rounded-xl shadow-lg shadow-gray-900/20 hover:bg-gray-800 transition-all active:scale-95"
              >
                <UserCircleIcon className="w-5 h-5" />
                <span>로그인</span>
              </button>
            )}
          </div>
        </header>

        <div className="flex-1 overflow-y-auto px-10 pb-10 pt-4">
            <div className="max-w-7xl mx-auto h-full">
                {currentTab === 'dashboard' && <LegacyDashboard data={csvData} />}
                {currentTab === 'map' && (
                    <div className="h-full">
                        <HeatmapView data={csvData} mode="interactive" />
                    </div>
                )}
                {currentTab === 'ai' && <AIAnalysisTab />}
                {currentTab === 'realtrade' && <RealTradeTab user={user} onOpenLogin={() => setIsLoginModalOpen(true)} />}
                {currentTab === 'favorites' && <FavoritesTab user={user} onOpenLogin={() => setIsLoginModalOpen(true)} />}
            </div>
        </div>
      </main>
    </div>
  );
}

const NavItem = ({ icon: Icon, text, active, onClick }) => (
  <div onClick={onClick} className={`flex items-center px-4 py-3.5 rounded-2xl cursor-pointer transition-all mb-1 group ${active ? 'bg-blue-50 text-blue-600 font-bold shadow-sm' : 'text-gray-500 hover:bg-gray-50 hover:text-gray-900'}`}>
    <Icon className={`w-5 h-5 mr-3 transition-colors ${active ? 'text-blue-600' : 'text-gray-400 group-hover:text-gray-600'}`} />
    <span className="text-sm">{text}</span>
  </div>
);

const TrendRow = ({ label, value }) => {
  const isPositive = parseFloat(value) > 0;
  return (
    <div className={`flex items-center ${label ? 'justify-between' : 'justify-end'} text-sm`}>
      {label && <span className="text-gray-400 font-medium">{label}</span>}
      <span className={`font-bold flex items-center ${isPositive ? 'text-red-500' : 'text-blue-500'}`}>
        {isPositive ? '+' : ''}{value}%
        <ArrowTrendingUpIcon className={`w-3 h-3 ml-1 ${isPositive ? '' : 'rotate-180'}`} />
      </span>
    </div>
  );
};

const StatCard = ({ title, value, subtext, trend, isPrediction }) => (
  <div className={`p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col justify-between hover:shadow-md transition-all ${isPrediction ? 'bg-gradient-to-br from-indigo-50 to-white' : 'bg-white'}`}>
    <div className="flex justify-between items-start">
      <h3 className="text-gray-500 text-xs font-bold uppercase tracking-wider">{title}</h3>
      {trend && (
        <span className={`text-xs font-bold flex items-center px-2 py-0.5 rounded-full ${trend.isPositive ? 'bg-red-50 text-red-500' : 'bg-blue-50 text-blue-500'}`}>
          {trend.isPositive ? '+' : ''}{trend.value}%
          <ArrowTrendingUpIcon className={`w-3 h-3 ml-1 ${trend.isPositive ? '' : 'rotate-180'}`} />
        </span>
      )}
    </div>
    <div className="mt-4">
      <span className="text-2xl font-bold text-gray-900 tracking-tight block">{value}</span>
      {subtext && <p className="text-xs text-gray-400 font-medium mt-1">{subtext}</p>}
    </div>
  </div>
);