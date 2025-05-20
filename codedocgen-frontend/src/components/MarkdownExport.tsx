import React from 'react';

interface MarkdownExportProps {
  content: string; // Markdown content to be exported
  fileName?: string; // Optional: name for the downloaded file
}

const MarkdownExport: React.FC<MarkdownExportProps> = ({ content, fileName = 'documentation.md' }) => {
  const handleExport = () => {
    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(link.href);
  };

  return (
    <button 
      onClick={handleExport}
      className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
    >
      Export as Markdown
    </button>
  );
};

export default MarkdownExport; 