import React from 'react';
import { Box, List, ListItem, ListItemIcon, ListItemText, Typography } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import ApiIcon from '@mui/icons-material/Api';
import ListAltIcon from '@mui/icons-material/ListAlt';
import CategoryIcon from '@mui/icons-material/Category';
import SchemaIcon from '@mui/icons-material/Schema';
import PublishIcon from '@mui/icons-material/Publish';
import ArticleIcon from '@mui/icons-material/Article';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import ClassIcon from '@mui/icons-material/Class';
import StorageIcon from '@mui/icons-material/Storage';

const NAV_ITEMS = [
  { label: 'Home', icon: <HomeIcon /> },
  { label: 'Overview', icon: <HomeIcon /> },
  { label: 'Endpoints', icon: <ApiIcon /> },
  { label: 'API Specs', icon: <ArticleIcon /> },
  { label: 'Call Flow', icon: <AccountTreeIcon /> },
  { label: 'Features', icon: <ListAltIcon /> },
  { label: 'Entities', icon: <CategoryIcon /> },
  { label: 'Database', icon: <StorageIcon /> },
  { label: 'All Classes', icon: <ClassIcon /> },
  { label: 'Diagrams', icon: <SchemaIcon /> },
  { label: 'Publish', icon: <PublishIcon /> },
];

function Sidebar({ activeSection, setActiveSection }) {
  return (
    <Box sx={{ width: 240, bgcolor: '#f4f6fa', borderRight: '1px solid #e0e0e0', py: 3, px: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', boxShadow: 1, minHeight: '100vh' }}>
      <Box sx={{ mb: 3, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Typography variant="h5" sx={{ fontWeight: 700, color: '#4f46e5', fontFamily: 'Quicksand', mb: 0.5 }}>CodeDocGen</Typography>
        <Typography variant="caption" sx={{ color: '#888', fontFamily: 'Quicksand' }}>Documentation Dashboard</Typography>
      </Box>
      <List sx={{ width: '100%', mt: 1 }}>
        {NAV_ITEMS.map((item) => (
          <ListItem button key={item.label} selected={activeSection === item.label} onClick={() => setActiveSection(item.label)} sx={{ borderRadius: 2, mb: 0.5, mx: 1, bgcolor: activeSection === item.label ? '#e0e7ff' : 'inherit', color: activeSection === item.label ? '#4f46e5' : '#333', transition: 'all 0.2s', fontFamily: 'Quicksand' }}>
            <ListItemIcon sx={{ color: activeSection === item.label ? '#4f46e5' : '#888' }}>{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} />
          </ListItem>
        ))}
      </List>
    </Box>
  );
}

export default Sidebar; 