/* Personal keys stay in memory and are sent only to Google's API. */
(function (root) {
  'use strict';
  const BASE = 'https://generativelanguage.googleapis.com/v1beta/';
  function classify(model) {
    const id = (model.name || '').replace(/^models\//, '');
    const methods = model.supportedGenerationMethods || [];
    const live = /live|native.audio/i.test(id) || methods.includes('bidiGenerateContent');
    const special = /embedding|imagen|veo|lyria|tts|image.generation|robotics|computer.use/i.test(id);
    const eligible = methods.includes('generateContent') && !live && !special && /^gemini-/i.test(id);
    return { id, eligible, label: live ? 'Live — שיחה בזמן אמת; לא לתרגום קבצים או בניית אפליקציות באתר' : /embedding/i.test(id) ? 'Embedding — חיפוש וייצוג טקסט; לא תרגום' : special ? 'מודל ייעודי לתמונה, קול, וידאו או כלים; אינו מתאים למסלול הטקסט באתר' : eligible ? 'מועמד לתרגום טקסט, קוד וצ׳אט — יש לבצע בדיקה' : 'שיטת הפעלה שאינה נתמכת באתר' };
  }
  function prepare(body, model) {
    const copy = JSON.parse(JSON.stringify(body));
    const config = copy.generationConfig || {};
    // Thinking options vary across generations. Let each model use its default.
    delete config.thinkingConfig;
    if (model.outputTokenLimit && config.maxOutputTokens) config.maxOutputTokens = Math.min(config.maxOutputTokens, model.outputTokenLimit);
    copy.generationConfig = config;
    return copy;
  }
  function create(fetcher) {
    let key = '', models = [], selected = { general: '', translation: '' }, revision = 0;
    const pending = new Set();
    function clear() { revision++; pending.forEach(c => c.abort()); pending.clear(); key = ''; models = []; selected = { general: '', translation: '' }; }
    function setKey(value) { clear(); key = String(value || '').trim(); if (!key || /\s/.test(key)) { key = ''; throw new Error('יש להדביק מפתח אחד, ללא רווחים פנימיים. גם מפתחות AQ נתמכים.'); } }
    async function request(path, body) {
      const ownKey = key, rev = revision;
      if (!ownKey) throw new Error('יש להזין מפתח אישי.');
      const ctrl = new AbortController(); pending.add(ctrl);
      const timer = setTimeout(() => ctrl.abort(), 90000);
      try {
        const response = await fetcher(BASE + path, { method: body ? 'POST' : 'GET', headers: { 'x-goog-api-key': ownKey, ...(body ? { 'Content-Type': 'application/json' } : {}) }, ...(body ? { body: JSON.stringify(body) } : {}), signal: ctrl.signal, credentials: 'omit', referrerPolicy: 'no-referrer' });
        if (rev !== revision) throw new Error('המפתח השתנה; יש לבצע בדיקה מחדש.');
        const data = await response.json().catch(() => { throw new Error('התקבלה תשובה שאינה JSON מגוגל. בדקו חיבור או סינון רשת.'); });
        if (!response.ok) {
          const message = {401:'המפתח לא התקבל בגוגל.',403:'אין הרשאה; בדקו הגבלות מפתח והפעלת Gemini API.',404:'המודל או פעולת ה־API אינם זמינים.',429:'המכסה או מגבלת הקצב מוצתה; זו אינה הוכחה שהמפתח שגוי.'}[response.status] || 'הבקשה לגוגל נכשלה.';
          throw new Error(message + ' (HTTP ' + response.status + ')');
        }
        return data;
      } catch (error) {
        if (error.name === 'AbortError') throw new Error('הבקשה בוטלה או חרגה מזמן ההמתנה.');
        if (error instanceof TypeError) throw new Error('לא ניתן להגיע לגוגל. בדקו חיבור, הגבלות דפדפן או סינון רשת.');
        throw error;
      } finally { clearTimeout(timer); pending.delete(ctrl); }
    }
    async function discover() {
      const found = [], seen = new Set(); let token = '';
      do {
        const data = await request('models?pageSize=1000' + (token ? '&pageToken=' + encodeURIComponent(token) : ''));
        for (const model of data.models || []) if (/^models\/[a-zA-Z0-9._-]+$/.test(model.name || '')) found.push(model);
        token = data.nextPageToken || '';
        if (token && seen.has(token)) throw new Error('גוגל החזירה עמוד חוזר; נסו לרענן את הרשימה.');
        seen.add(token);
      } while (token);
      models = [...new Map(found.map(m => [m.name, m])).values()];
      const first = models.find(m => classify(m).eligible);
      selected.general = selected.translation = first ? classify(first).id : '';
      return models.slice();
    }
    function choose(task, id) {
      if (!Object.hasOwn(selected, task)) throw new Error('סוג משימה לא מוכר.');
      const model = models.find(m => classify(m).id === id);
      if (!model || !classify(model).eligible) throw new Error('המודל אינו מתאים לתרגום או ליצירת קוד במסלול הזה.');
      selected[task] = id;
    }
    async function generate(body, task = 'general') {
      const model = models.find(m => classify(m).id === selected[task]);
      if (!model || !classify(model).eligible) throw new Error('יש לבדוק את המפתח ולבחור מודל בחיבורים.');
      return request(model.name + ':generateContent', prepare(body, model));
    }
    async function test(task) {
      const data = await generate({ contents:[{role:'user',parts:[{text: task === 'translation' ? 'Translate hello to Hebrew. Return only JSON with a text field.' : 'Return only JSON with a text field containing OK.'}]}], generationConfig:{responseMimeType:'application/json',maxOutputTokens:1024} }, task);
      const raw = (data.candidates?.[0]?.content?.parts || []).filter(p => !p.thought).map(p => p.text || '').join('');
      let result; try { result = JSON.parse(raw); } catch (_) { throw new Error('המודל לא החזיר JSON תקין בבדיקה; לא אומתה התאמה למסלול האתר.'); }
      if (typeof result.text !== 'string' || !result.text.trim()) throw new Error('הבדיקה לא החזירה תשובת טקסט.');
      return result.text;
    }
    return { clear, setKey, discover, choose, generate, test, hasKey: () => !!key, getModels: () => models.slice() };
  }
  root.PersonalGemini = { classify, prepare, create };
  if (typeof module !== 'undefined') module.exports = root.PersonalGemini;
})(typeof window !== 'undefined' ? window : globalThis);
