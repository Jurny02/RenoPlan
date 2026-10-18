// Renders the RenoPlan app icons and the iOS launch logo as PNGs.
// Run from the repository root: `swift scripts/render-icons.swift .`
// Geometry is in a 108×108 viewport (Android adaptive icon units), the same as
// androidApp/src/main/res/drawable/ic_launcher_foreground.xml; change both together.
import AppKit

let brand = NSColor(srgbRed: 0x9A / 255.0, green: 0x45 / 255.0, blue: 0x21 / 255.0, alpha: 1)

enum Shape { case square, roundedSquare, circle, none }

func glyph(in ctx: CGContext) {
    ctx.setStrokeColor(NSColor.white.cgColor)
    ctx.setLineJoin(.round)
    ctx.setLineCap(.round)
    // Floor plan with a notch in one corner.
    ctx.setLineWidth(5)
    ctx.move(to: CGPoint(x: 32, y: 40))
    for p in [(76, 40), (76, 66), (64, 66), (64, 78), (32, 78)] { ctx.addLine(to: CGPoint(x: p.0, y: p.1)) }
    ctx.closePath()
    ctx.strokePath()
    // Dimension line with end ticks.
    ctx.setLineWidth(3)
    ctx.move(to: CGPoint(x: 32, y: 30)); ctx.addLine(to: CGPoint(x: 76, y: 30))
    ctx.move(to: CGPoint(x: 32, y: 26)); ctx.addLine(to: CGPoint(x: 32, y: 34))
    ctx.move(to: CGPoint(x: 76, y: 26)); ctx.addLine(to: CGPoint(x: 76, y: 34))
    ctx.strokePath()
}

/// Draws the viewport region [crop, 108 - crop] into a size×size PNG.
func render(size: Int, crop: CGFloat, shape: Shape, background: Bool, to path: String) {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size, bitsPerSample: 8,
                               samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
                               bytesPerRow: 0, bitsPerPixel: 0)!
    let ctx = NSGraphicsContext(bitmapImageRep: rep)!.cgContext
    let s = CGFloat(size)
    let rect = CGRect(x: 0, y: 0, width: s, height: s)
    switch shape {
    case .circle: ctx.addEllipse(in: rect); ctx.clip()
    case .roundedSquare: ctx.addPath(CGPath(roundedRect: rect, cornerWidth: s * 0.18, cornerHeight: s * 0.18, transform: nil)); ctx.clip()
    default: break
    }
    if background { ctx.setFillColor(brand.cgColor); ctx.fill(rect) }
    // Flip to a top-left origin like Android vectors, then map the cropped viewport onto the image.
    let scale = s / (108 - 2 * crop)
    ctx.translateBy(x: 0, y: s); ctx.scaleBy(x: scale, y: -scale); ctx.translateBy(x: -crop, y: -crop)
    glyph(in: ctx)
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: path))
}

let out = CommandLine.arguments[1]
let res = "\(out)/androidApp/src/main/res"
for (dir, px) in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)] {
    // Legacy icons (API 24–25) show the middle 72 units of the adaptive canvas.
    render(size: px, crop: 18, shape: .roundedSquare, background: true, to: "\(res)/mipmap-\(dir)/ic_launcher.png")
    render(size: px, crop: 18, shape: .circle, background: true, to: "\(res)/mipmap-\(dir)/ic_launcher_round.png")
}
let assets = "\(out)/iosApp/iosApp/Assets.xcassets"
// iOS masks the corners itself, so the app icon is a full square.
render(size: 1024, crop: 20, shape: .square, background: true, to: "\(assets)/AppIcon.appiconset/app-icon-1024.png")
// Launch screen logo: white glyph on transparent, 120 pt.
for (suffix, px) in [("", 120), ("@2x", 240), ("@3x", 360)] {
    render(size: px, crop: 20, shape: .none, background: false, to: "\(assets)/LaunchLogo.imageset/launch-logo\(suffix).png")
}
print("ok")
