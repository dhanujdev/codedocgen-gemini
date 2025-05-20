import React, { useEffect, useRef } from 'react';
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch';
import mermaid from 'mermaid';

// Define a more specific type for a single diagram object
export interface Diagram {
  key?: string; // Optional, but good for lists if we ever revert
  title: string;
  url?: string;        // For image-based diagrams
  content?: string;    // For text-based diagrams (Mermaid/PlantUML)
  type: string;
  originalTitle?: string; 
}

interface DiagramViewerProps {
  diagram: Diagram | null; // Expect a single diagram object, or null
}

// Initialize Mermaid once
mermaid.initialize({
  startOnLoad: false, // We will render manually
  theme: 'neutral', // or 'default', 'forest', 'dark', 'neutral' - choose one that fits
  // securityLevel: 'loose', // Consider security implications if diagrams come from untrusted sources
  // loglevel: 'debug', // For debugging mermaid issues
});

const DiagramViewer: React.FC<DiagramViewerProps> = ({ diagram }) => {
  const mermaidDivRef = useRef<HTMLDivElement>(null); // Ref for the div where Mermaid SVG will be injected

  useEffect(() => {
    if (diagram && diagram.content && mermaidDivRef.current) {
      const currentMermaidDiv = mermaidDivRef.current; // Capture ref value
      // Clear previous content before rendering new one
      currentMermaidDiv.innerHTML = ''; 
      try {
        // Attempt to have Mermaid render directly into the container
        // The ID for the graph definition within the SVG, must be unique if multiple graphs are on the page simultaneously.
        // However, since we clear and re-render one diagram at a time, a static ID might be fine.
        // To be safe, using a dynamic one related to the diagram or a timestamp.
        const graphId = `mermaid-graph-${diagram.key || Date.now()}`;
        mermaid.render(graphId, diagram.content, currentMermaidDiv);
      } catch (error) {
        console.error("Mermaid rendering error:", error);
        if (currentMermaidDiv) {
          currentMermaidDiv.innerHTML = '<p style="color: red;">Error rendering Mermaid diagram.</p>';
        }
      }
    } else if (mermaidDivRef.current) {
      // Clear previous diagram if no diagram/content
      mermaidDivRef.current.innerHTML = '';
    }
  }, [diagram]); // Rerun when the diagram (and thus its content) changes

  // If no diagram is provided, or diagram is explicitly null
  if (!diagram) {
    // This message is usually handled by the parent (Diagrams.js) before calling DiagramViewer
    // But as a fallback, or if DiagramViewer could be used directly with a null diagram:
    return <p style={{ textAlign: 'center', marginTop: '20px' }}>No diagram selected or available to display.</p>;
  }

  // The parent component (Diagrams.js) already has a title for the section ("System Diagrams")
  // So the h2 "Diagrams" here might be redundant or could be more specific.
  // For now, let's display the specific title of the selected diagram.
  return (
    <div style={{ width: '100%' }}> {/* Ensure the viewer itself takes full width */}
      {/* The h2 "Diagrams" might be redundant if Diagrams.js already has a main title for the section. 
          Consider removing or making it specific to the selected diagram. 
          For now, commenting out the generic "Diagrams" h2 as the parent handles section title.
      <h2 className="text-xl font-semibold mb-2">Diagrams</h2> 
      */}
      
      {/* Removed the grid layout as we are displaying a single diagram */}
      <div className="border p-2 rounded" style={{ width: '100%' }}>
        <h3 className="text-lg font-medium mb-1" style={{ textAlign: 'center' }}>{diagram.title || 'Untitled Diagram'}</h3>
        {diagram.url ? (
          <TransformWrapper
            initialScale={1}
            // minScale={0.5} // Optional: set min scale
            // maxScale={3}   // Optional: set max scale
            // limitToBounds={true} // Optional: prevent panning outside content
            // wheel={{ step: 0.2 }} // Optional: control zoom sensitivity
            // doubleClick={{ mode: 'reset' }} // Optional: reset on double click
          >
            <TransformComponent
              // wrapperStyle={{ width: "100%", height: "auto" }} // Ensure wrapper takes space
              // contentStyle={{ width: "100%", height: "auto" }} // Ensure content uses that space
            >
              <img 
                src={diagram.url} 
                alt={diagram.title || 'Diagram'} 
                className="w-full h-auto" // Removed 'border' from here, outer div has it
                style={{ maxWidth: '100%', display: 'block', margin: '0 auto' }} 
              />
            </TransformComponent>
          </TransformWrapper>
        ) : diagram.content ? (
          // For Mermaid content, zoom/pan can be applied to the div containing the SVG if needed
          // For now, letting the SVG scale with its container.
          // If react-zoom-pan-pinch were to be used here, it would wrap this div.
          <div ref={mermaidDivRef} className="mermaid-container w-full" style={{ textAlign: 'center' }}>
            {/* Mermaid SVG will be generated and inserted here by mermaid.render directly */}
          </div>
        ) : (
          <p>Diagram content not available (no URL or text content).</p>
        )}
      </div>
    </div>
  );
};

export default DiagramViewer;
