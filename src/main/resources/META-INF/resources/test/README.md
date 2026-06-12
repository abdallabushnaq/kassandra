# Test Pages

This directory contains standalone HTML test pages for testing chart components without running the full Kassandra application.

## Sprints Overview Test

**File:** `sprints-overview-test.html`

**Purpose:** Test the SprintsOverview chart with mock data scenarios.

### How to Use

#### Option 1: Via Running Application

If Kassandra is running on port 8080:
```
http://localhost:8080/test/sprints-overview-test.html
```

#### Option 2: Standalone Web Server

From this directory's parent:
```bash
cd E:/github/kassandra/src/main/resources/META-INF/resources
python -m http.server 8000
```

Then open:
```
http://localhost:8000/test/sprints-overview-test.html
```

#### Option 3: Direct File Access

Some browsers allow opening `sprints-overview-test.html` directly:
```
file:///E:/github/kassandra/src/main/resources/META-INF/resources/test/sprints-overview-test.html
```

(May have CORS issues depending on browser security settings)

### Available Test Scenarios

- **Normal**: 5 lanes, 10 sprints, 6-month timeline
- **Dense**: 10 lanes, 30 sprints (stress test)
- **Sparse**: 2 lanes, 3 sprints (minimal)
- **Long Timeline**: 12-month period, 40 sprints
- **Overlapping**: Complex lane distribution

### Controls

- **Scenario Dropdown**: Switch between test scenarios
- **Reload Chart**: Regenerate mock data
- **Reset Zoom**: Return to default zoom level
- **Mouse Wheel**: Zoom in/out
- **Click + Drag**: Pan left/right
- **Double-Click**: Reset zoom (same as button)

### What Gets Tested

- ✅ Initial render
- ✅ Calendar axes (year/month/week/day rows)
- ✅ Weekend background stripes
- ✅ Sprint rectangles with labels
- ✅ Now-line (current date indicator)
- ✅ Grid lines
- ✅ Zoom behavior
- ✅ Pan behavior
- ✅ Tooltips
- ✅ Performance (console.time logs)

### Debugging

Open browser DevTools (F12) to:
- Check console for errors
- Inspect SVG structure
- Profile performance
- Monitor network requests (should be none after initial load)
- Examine D3 selections

### Mock Data Structure

The test page generates data matching the API contract:
```json
{
  "lanes": [
    {
      "laneId": 0,
      "sprints": [
        {
          "id": 1,
          "key": "S-1",
          "name": "Sprint Name",
          "start": "2026-06-01T00:00:00",
          "end": "2026-06-14T23:59:59",
          "status": "STARTED",
          "color": "#1f8fff50",
          "hasGantt": true,
          "delay": false
        }
      ]
    }
  ],
  "meta": {
    "chartStart": "2026-04-01T00:00:00",
    "chartEnd": "2026-10-31T23:59:59",
    "now": "2026-06-12T14:30:00",
    "laneCount": 5,
    "xAxesTheme": { /* theme colors */ }
  }
}
```

This matches the real API endpoint `/api/overview/sprints`.

### Modifying Test Data

Edit `generateMockData()` function in the HTML file to:
- Change date ranges
- Add/remove sprints
- Modify colors
- Adjust lane distribution
- Test edge cases

### Known Limitations

- Mock data only (no real backend)
- No persistence (refreshing resets)
- Context menu not implemented yet (V4 feature)
- Export not available (V4 feature)

### Performance Expectations

| Scenario | Expected Render Time |
|----------|---------------------|
| Normal | < 100ms |
| Dense | < 250ms |
| Long Timeline | < 300ms |

Times measured on mid-range hardware. Use DevTools Performance tab for detailed profiling.

### Troubleshooting

**Chart doesn't render:**
- Check console for JavaScript errors
- Verify `sprints-overview-v3.js` exists in parent `js/` directory
- Ensure D3.js CDN is accessible

**Chart looks wrong:**
- Verify mock data structure matches API contract
- Check theme colors in `xAxesTheme`
- Inspect SVG with DevTools

**Performance issues:**
- Try "Sparse" scenario first
- Check browser (Chrome/Edge recommended)
- Close other tabs
- Use DevTools Performance profiler

### Adding More Tests

To add a new test scenario:

1. Edit `sprints-overview-test.html`
2. Add option to `<select id="scenario">`:
   ```html
   <option value="my-scenario">My Scenario</option>
   ```
3. Add case to `generateMockData()`:
   ```javascript
   case 'my-scenario':
       // Generate custom data
       break;
   ```

### Related Documentation

- Full docs: `../../docs/sprints-overview-v3-documentation.md`
- Quick start: `../../docs/sprints-overview-v3-quickstart.md`
- Complete summary: `../../docs/SPRINTS-OVERVIEW-V3-COMPLETE.md`

### Questions?

See the comprehensive documentation in `docs/` or check the source code comments in `js/sprints-overview-v3.js`.

