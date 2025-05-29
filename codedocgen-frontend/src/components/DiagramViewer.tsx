import React, { useEffect, useRef, useState } from 'react';
import { TransformWrapper, TransformComponent, useControls } from 'react-zoom-pan-pinch';
import mermaid, { MermaidConfig } from 'mermaid';
import { Box, IconButton, Tooltip, Paper, Typography } from '@mui/material';
import ZoomInIcon from '@mui/icons-material/ZoomIn';
import ZoomOutIcon from '@mui/icons-material/ZoomOut';
import DownloadIcon from '@mui/icons-material/Download';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import RefreshIcon from '@mui/icons-material/Refresh'; // For resetting zoom

// Define a more specific type for a single diagram object
export interface Diagram {
  key?: string;
  title: string;
  url?: string;
  content?: string;
  type: string;
  originalTitle?: string;
}

interface DiagramViewerProps {
  diagram: Diagram | null;
}

// Define the configuration for Mermaid
const mermaidConfig: MermaidConfig = {
  startOnLoad: false,
  theme: 'neutral',
  // securityLevel: 'sandbox', // Example: for stricter security
  // Other valid MermaidConfig properties can be added here
};
mermaid.initialize(mermaidConfig);

const ViewerControls = () => {
  const { zoomIn, zoomOut, resetTransform } = useControls();
  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', gap: 1, mb: 1 }}>
      <Tooltip title="Zoom In">
        <IconButton onClick={() => zoomIn(0.2)} size="small"><ZoomInIcon /></IconButton>
      </Tooltip>
      <Tooltip title="Zoom Out">
        <IconButton onClick={() => zoomOut(0.2)} size="small"><ZoomOutIcon /></IconButton>
      </Tooltip>
      <Tooltip title="Reset Zoom">
        <IconButton onClick={() => resetTransform()} size="small"><RefreshIcon /></IconButton>
      </Tooltip>
    </Box>
  );
};

const DiagramViewer: React.FC<DiagramViewerProps> = ({ diagram }) => {
  const mermaidDivRef = useRef<HTMLDivElement>(null);
  const [svgContent, setSvgContent] = useState<string | null>(null);
  const [renderError, setRenderError] = useState<string | null>(null);

  useEffect(() => {
    setSvgContent(null); // Clear previous SVG
    setRenderError(null); // Clear previous error

    const renderMermaid = async () => {
      if (diagram && diagram.content && mermaidDivRef.current) {
        const currentMermaidDiv = mermaidDivRef.current;
        currentMermaidDiv.innerHTML = ''; // Clear the container explicitly
        try {
          const graphId = `mermaid-graph-${diagram.key || Date.now()}`;
          // Use mermaidAPI.render for more control and to get SVG directly
          // It returns a promise, so we need to await it.
          const { svg } = await mermaid.mermaidAPI.render(graphId, diagram.content);
          setSvgContent(svg);
          currentMermaidDiv.innerHTML = svg; // Inject the SVG for display

          // Ensure the SVG itself is responsive
          const svgElement = currentMermaidDiv.querySelector('svg');
          if (svgElement) {
            svgElement.style.maxWidth = '100%';
            svgElement.style.height = 'auto';
            svgElement.removeAttribute('height');
            svgElement.setAttribute('width', '100%'); // Ensure it scales with the parent
          }
        } catch (error: any) {
          console.error("Mermaid rendering error:", error);
          const errorMessage = error.message || "Failed to render Mermaid diagram.";
          setRenderError(errorMessage);
          setSvgContent(null);
          if (currentMermaidDiv) {
            currentMermaidDiv.innerHTML = `<p style="color: red;">${errorMessage}</p>`;
          }
        }
      } else if (mermaidDivRef.current) {
        mermaidDivRef.current.innerHTML = ''; // Clear if no diagram or content
      }
    };

    renderMermaid();

  }, [diagram]);

  const handleDownload = () => {
    if (diagram && svgContent && !renderError && diagram.type !== 'IMAGE') {
      const blob = new Blob([svgContent], { type: 'image/svg+xml' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${diagram.title.replace(/\s+/g, '_') || 'diagram'}.svg`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } else if (diagram && diagram.url && diagram.type === 'IMAGE') {
      const a = document.createElement('a');
      a.href = diagram.url;
      a.download = diagram.title.replace(/\s+/g, '_') || 'diagram';
      a.target = '_blank';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    }
  };

  const handleOpenInNewTab = () => {
    if (diagram && svgContent && !renderError && diagram.type !== 'IMAGE') {
      const svgDataUrl = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svgContent)}`;
      window.open(svgDataUrl, '_blank');
    } else if (diagram && diagram.url && diagram.type === 'IMAGE') {
      window.open(diagram.url, '_blank');
    }
  };

  if (!diagram) {
    return <Typography sx={{ textAlign: 'center', mt: 4 }}>No diagram selected or available.</Typography>;
  }

  const isMermaid = diagram.content && diagram.type !== 'IMAGE';
  const isImage = diagram.url && diagram.type === 'IMAGE';

  return (
    <Paper elevation={1} sx={{ p: 2, width: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <Typography variant="h6" sx={{ fontWeight: 600, mb: 1, textAlign: 'center' }}>
        {diagram.title || 'Untitled Diagram'}
      </Typography>

      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 1, mb: 1 }}>
        {( (isMermaid && svgContent && !renderError) || isImage) && (
          <Tooltip title="Download Diagram">
            <IconButton onClick={handleDownload} size="small"><DownloadIcon /></IconButton>
          </Tooltip>
        )}
        {( (isMermaid && svgContent && !renderError) || isImage) && (
          <Tooltip title="Open in New Tab">
            <IconButton onClick={handleOpenInNewTab} size="small"><OpenInNewIcon /></IconButton>
          </Tooltip>
        )}
      </Box>

      <TransformWrapper
        initialScale={1}
        minScale={0.2}
        maxScale={5}
        limitToBounds={false}
        wheel={{ step: 0.2 }}
        doubleClick={{ mode: 'reset' }}
        key={diagram.key || diagram.title} // Add key to force re-render of zoom-pan-pinch on diagram change
      >
        <ViewerControls /> 
        <TransformComponent
          wrapperStyle={{ width: '100%', minHeight: '300px', border: '1px solid #eee', borderRadius: '4px' }}
          contentStyle={{ width: '100%', height: '100%' }}
        >
          {isImage && diagram.url && (
            <img 
              src={diagram.url} 
              alt={diagram.title || 'Diagram'} 
              style={{ width: '100%', height: 'auto', display: 'block'}} 
            />
          )}
          {isMermaid && (
            <div 
              ref={mermaidDivRef} 
              className="mermaid-diagram-container"
              style={{ width: '100%', minHeight: '300px'}} // Ensure div takes space
            >
              {renderError && <Typography sx={{textAlign: 'center', p:2, color: 'red'}}>{renderError}</Typography>}
              {!svgContent && !renderError && <Typography sx={{textAlign: 'center', p:2}}>Loading diagram...</Typography>}
              {/* SVG content is injected by useEffect into this div */}
            </div>
          )}
        </TransformComponent>
      </TransformWrapper>

      {!isImage && !isMermaid && !renderError && (
        <Typography sx={{textAlign: 'center', mt:2 }}>Diagram content not available (no URL or text content).</Typography>
      )}
    </Paper>
  );
};

export default DiagramViewer;

